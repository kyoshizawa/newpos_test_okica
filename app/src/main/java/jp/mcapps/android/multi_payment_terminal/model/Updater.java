package jp.mcapps.android.multi_payment_terminal.model;

import android.app.Application;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.amplifyframework.api.rest.RestOptions;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.storage.operation.StorageDownloadFileOperation;
import com.amplifyframework.storage.options.StorageDownloadFileOptions;
import com.google.gson.Gson;
import com.pos.device.sys.SystemManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import androidx.core.content.FileProvider;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BuildConfig;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.data.DeviceServiceInfo;
import jp.mcapps.android.multi_payment_terminal.data.FirmWareInfo;
import jp.mcapps.android.multi_payment_terminal.model.device_network_manager.DeviceNetworkManager;
import jp.mcapps.android.multi_payment_terminal.util.DeviceUtils;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.IFBoxApi;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.IFBoxApiImpl;
import jp.mcapps.android.multi_payment_terminal.webapi.ifbox.data.Version;
import timber.log.Timber;

@SuppressWarnings("ALL")
public class Updater {
    private final Application _app = MainApplication.getInstance();
    private final File _localDir = _app.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
    private final Gson _gosn = new Gson();
    private List<FirmWareInfo> _updateList = new ArrayList<>();

    public enum Products {
        None,
        ThisApp,
        IFBox,
    }

    public static class DownloadProgress {
        public boolean isEnd = false;
        public boolean isSuccess = false;
        public int progress = 0;
        public Exception ex = null;
        public Products product = Products.None;
        public int fileNo = 0;
        public int totalFileNum = 0;

        public DownloadProgress(boolean isEnd, boolean isSuccess, int progress, Exception ex, Products product, int fileNo, int totalFilesNum) {
            this.isEnd = isEnd;
            this.isSuccess = isSuccess;
            this.progress = progress;
            this.ex = ex;
            this.product = product;
            this.fileNo = fileNo;
            this.totalFileNum = totalFilesNum;
        }

        public DownloadProgress(boolean isEnd, boolean isSuccess, int progress, Products product, int fileNo, int totalFilesNum) {
            new DownloadProgress(isEnd, isSuccess, progress, null, product, fileNo, totalFilesNum);
        }
    }

    public static class NewVersionNotExistsException extends Throwable {
    }

    private StorageDownloadFileOperation _operation;
    private DeviceNetworkManager _deviceNetworkManager = null;

    public Updater(DeviceNetworkManager deviceNetworkManager) {
        _deviceNetworkManager = deviceNetworkManager;
    }

    public boolean isForcedUpdate() {
        if (_updateList.size() == 0) {
            return false;
        }

        for (int i = 0; i < _updateList.size(); i++) {
            FirmWareInfo firmWareInfo = _updateList.get(i);

            // とりあえず強制アップデートフラグが立っていれば強制アップデートさせる
            // メインアプリ
            if (firmWareInfo.modelName.equals(_app.getPackageName())
                    && firmWareInfo.isForceUpdate) {
                return true;
            }

            // プリペイドアプリ
            if (firmWareInfo.modelName.equals(BuildConfig.PREPAID_APP_MODEL)
                    && firmWareInfo.isForceUpdate) {
                return true;
            }
        }

        return false;
    }

    public Single<List<FirmWareInfo>> getAllLatestIFBoxVersion() {
        Timber.i("IM-A820 最新バージョン確認");

        final Map<String, String> params = new HashMap<String, String>() {
            {
                put("latest", "");
                put("or", "(product_name.eq.IM-A820)");
            }
        };

        final RestOptions options = RestOptions.builder()
                .addPath("/firmwares")
                .addQueryParameters(params)
                .build();

        return Single.create(emitter -> {
            Amplify.API.get(options,
                    response -> {
                        final String json = response.getData().asString();
                        Timber.d("checkVersion succeeded: %s", json);

                        FirmWareInfo[] firmwares = _gosn.fromJson(json, FirmWareInfo[].class);
                        firmwares = firmwares != null ? firmwares : new FirmWareInfo[]{};
                        emitter.onSuccess(Arrays.asList(firmwares));
                    },
                    error -> {
                        Timber.e(error);
                        emitter.onError(error);
                    }
            );
        });
    }

    public boolean getPrepaidAppVersion() {
        boolean isPrepaid = false;

        PackageManager packageManager = _app.getPackageManager();
        try {
            PackageInfo packageInfo;
            // Android 12 (API 32) 以前
            @SuppressWarnings("deprecation")
            PackageInfo tempInfo = packageManager.getPackageInfo(BuildConfig.PREPAID_APP_MODEL, 0);
            packageInfo = tempInfo;

            String versionName = packageInfo.versionName;
            long versionCode;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Android 9 (API 28) 以降
                versionCode = packageInfo.getLongVersionCode();
            } else {
                // Android 8 (API 27) 以前
                @SuppressWarnings("deprecation")
                int tempVersionCode = packageInfo.versionCode;
                versionCode = tempVersionCode;
            }
            AppPreference.setPrepaidAppVersionCode(versionCode);
            AppPreference.setPrepaidAppVersionName(versionName);
            isPrepaid = true;

        } catch (PackageManager.NameNotFoundException e) {
            // プリペイドアプリがインストールされてない
            AppPreference.setPrepaidAppVersionCode(0L);
            AppPreference.setPrepaidAppVersionName("0.0.0");

            if (!AppPreference.getIsPrepaid()) {
                return isPrepaid;
            }
            isPrepaid = true;
        }

        return isPrepaid;
    }

    public Single<Boolean> getPrepaidAppLatestVersion(Boolean isPreUpdate) {
        final  StringBuilder sb = new StringBuilder();
        sb.append("(");

        // プリペイドアプリを使えるか判定
        Boolean isPrepaid = getPrepaidAppVersion();
        if (isPrepaid) {
            sb.append(String.format("model_name.eq.%s", BuildConfig.PREPAID_APP_MODEL));
        }

        sb.append(")");

        final Map<String, String> params = new HashMap<String, String>() {
            private final List<String> iTagList = new ArrayList<String>();

            {
                put("latest", "");
                put("or", sb.toString());

                iTagList.add("SERIAL:" + DeviceUtils.getSerial());
                if (isPreUpdate) {
                    iTagList.add("preview");
                }

                if (iTagList.size() > 0) {
                    final StringBuilder iTags = new StringBuilder();

                    for (int i = 0; i < iTagList.size(); i++) {
                        iTags.append(i == 0 ? "" : "&i-tags=").append(iTagList.get(i));
                    }

                    put("i-tags", iTags.toString());
                }
            }
        };

        final RestOptions options = RestOptions.builder()
                .addPath("/firmwares")
                .addQueryParameters(params)
                .build();

        // 戻り値は更新後に再起動するかどうか
        return Single.create(emitter -> {
            Amplify.API.get(options,
                    response -> {
                        boolean needsApkReboot = false;
                        _updateList.clear();

                        final String json = response.getData().asString();
                        Timber.d("checkVersion succeeded: %s", json);

                        final FirmWareInfo[] firmwares = _gosn.fromJson(json, FirmWareInfo[].class);

                        if (firmwares == null) {
                            if (!emitter.isDisposed()) {
                                emitter.onError(new NewVersionNotExistsException());
                            }
                            return;
                        }

                        for (FirmWareInfo firmware : firmwares) {
                            int v = firmware.versionCode;

                            // プリペイド利用フラグがONでバージョンが自分より大きい（未インストールの場合はバージョンが0）
                            boolean isPrepaidUpdateExists = AppPreference.getIsPrepaid()
                                    && firmware.modelName.equals(BuildConfig.PREPAID_APP_MODEL)
                                    && firmware.versionCode > AppPreference.getPrepaidAppVersionCode();

                            needsApkReboot = false;

                            if (isPrepaidUpdateExists) _updateList.add(firmware);

                            // 強制アップデートのチェック
                            if (firmware.tags.size() > 0) {
                                for (String tag : firmware.tags) {
                                    // タグにForceUpdateがあれば強制アップデート対象
                                    if (tag.equals("ForceUpdate")) {
                                        firmware.isForceUpdate = true;
                                    }
                                }
                            }
                        }

                        if (_updateList.size() > 0) {
                            if (!emitter.isDisposed()) {
                                emitter.onSuccess(needsApkReboot);
                            }
                        } else {
                            // エラーで返したくないけどnullで値返せないので
                            if (!emitter.isDisposed()) {
                                emitter.onError(new NewVersionNotExistsException());
                            }
                        }
                    },
                    error -> {
                        Timber.e(error);
                        if (!emitter.isDisposed()) {
                            emitter.onError(error);
                        }
                    }
            );
        });
    }

    public Single<Boolean> getLatestVersion(boolean isPreUpdate, boolean isAllApp) {
        Timber.i("APK 最新バージョン確認");
        final StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append(String.format("model_name.eq.%s", _app.getPackageName()));
        if (AppPreference.isIFBoxHost()) {
            final FirmWareInfo i = AppPreference.getIFBoxOTAInfo();
            sb.append(String.format(",model_name.eq.%s", i.modelName));
        }

        if (isAllApp) {
            // プリペイドアプリを使えるか判定
            Boolean isPrepaid = getPrepaidAppVersion();

            if (isPrepaid) {
                sb.append(String.format(",model_name.eq.%s", BuildConfig.PREPAID_APP_MODEL));
            }
        }

        sb.append(")");

        final Map<String, String> params = new HashMap<String, String>() {
            private final List<String> iTagList = new ArrayList<String>();

            {
                put("latest", "");
                put("or", sb.toString());

                iTagList.add("SERIAL:" + DeviceUtils.getSerial());
                if (isPreUpdate) {
                    iTagList.add("preview");
                }

                if (iTagList.size() > 0) {
                    final StringBuilder iTags = new StringBuilder();

                    for (int i = 0; i < iTagList.size(); i++) {
                        iTags.append(i == 0 ? "" : "&i-tags=").append(iTagList.get(i));
                    }

                    put("i-tags", iTags.toString());
                }
            }
        };

        final RestOptions options = RestOptions.builder()
                .addPath("/firmwares")
                .addQueryParameters(params)
                .build();

        // 戻り値は更新後に再起動するかどうか
        return Single.create(emitter -> {
            Amplify.API.get(options,
                    response -> {
                        boolean needsApkReboot = false;
                        _updateList.clear();

                        final String json = response.getData().asString();
                        Timber.d("checkVersion succeeded: %s", json);

                        final FirmWareInfo[] firmwares = _gosn.fromJson(json, FirmWareInfo[].class);

                        if (firmwares == null) {
                            if (!emitter.isDisposed()) {
                                emitter.onError(new NewVersionNotExistsException());
                            }
                            return;
                        }

                        for (FirmWareInfo firmware : firmwares) {
                            int v = firmware.versionCode;

                            boolean isApkUpdateExists = firmware.modelName.equals(_app.getPackageName())
                                    && firmware.versionCode > BuildConfig.VERSION_CODE;

                            boolean isIFBoxUpdateExists = AppPreference.isIFBoxHost()
                                    && firmware.modelName.equals(AppPreference.getIFBoxOTAInfo().modelName)
                                    && firmware.versionCode > AppPreference.getIFBoxOTAInfo().versionCode;

                            // 自分よりバージョンが大きいか確認（未インストールの場合はバージョンが0なのでそれは無視）
                            boolean isPrepaidUpdateExists = firmware.modelName.equals(BuildConfig.PREPAID_APP_MODEL)
                                    && firmware.versionCode > AppPreference.getPrepaidAppVersionCode()
                                    && AppPreference.getPrepaidAppVersionCode() > 0;

                            needsApkReboot = isApkUpdateExists;

                            if (isApkUpdateExists || isIFBoxUpdateExists || isPrepaidUpdateExists) _updateList.add(firmware);
//                            if (isApkUpdateExists || isIFBoxUpdateExists) _updateList.add(firmware);

                            // 強制アップデートのチェック
                            if (firmware.tags.size() > 0) {
                                for (String tag : firmware.tags) {
                                    // タグにForceUpdateがあれば強制アップデート対象
                                    if (tag.equals("ForceUpdate")) {
                                        firmware.isForceUpdate = true;
                                    }
                                }
                            }
                        }

                        if (_updateList.size() > 0) {
                            if (!emitter.isDisposed()) {
                                emitter.onSuccess(needsApkReboot);
                            }
                        } else {
                            // エラーで返したくないけどnullで値返せないので
                            if (!emitter.isDisposed()) {
                                emitter.onError(new NewVersionNotExistsException());
                            }
                        }
                    },
                    error -> {
                        Timber.e(error);
                        if (!emitter.isDisposed()) {
                            emitter.onError(error);
                        }
                    }
            );
        });
    }

    private interface DownloadTask {
        void run(int i);
    }

    public Observable<DownloadProgress> downloadIFBox(String s3KeyName, String localFileName) {
        return Observable.create(emitter -> {

            final String targetFilename = _localDir.toString() + File.separator + localFileName;
            final String tempFilename = targetFilename + ".download";
            final StorageDownloadFileOptions  options = StorageDownloadFileOptions.defaultInstance();

            _operation = Amplify.Storage.downloadFile(
                    s3KeyName,
                    new File(tempFilename),
                    options,
                    progress -> {
                        Timber.d("Amplify.Storage.downloadFile.onProgress(): %s", progress);

                        // ダウンロード中
                        emitter.onNext(new DownloadProgress(false, false, (int) (progress.getFractionCompleted() * 100), null, Products.IFBox, 1, 1));
                    },
                    result -> {
                        Timber.d("Amplify.Storage.downloadFile.onSuccess(): %s", result);
                        // ファイル名をリネームし、正式なものへ変更

                        new File(tempFilename).renameTo(new File(targetFilename));
                        Timber.d("apk path: %s", targetFilename);

                        emitter.onComplete();
                    },
                    error -> {
                        Timber.e(error, "Amplify.Storage.downloadFile.onError()");
                        if (!emitter.isDisposed()) {
                            emitter.onError(new Throwable());
                        }
                    }
            );
        });
    }

    public Observable<DownloadProgress> download() {
        return Observable.create(emitter -> {
            if (_updateList.size() <= 0) {
                if (!emitter.isDisposed()) {
                    emitter.onError(new IllegalStateException("update not exists"));
                }
                return;
            }


            final DownloadTask downloadTask = new DownloadTask() {
                @Override
                public void run(int i) {
                    if (emitter.isDisposed()) return;

                    final int fileNo = i + 1;
                    final FirmWareInfo firmware = _updateList.get(i);

                    final boolean isApk = firmware.modelName.equals(_app.getPackageName());
                    final boolean isIFBox = AppPreference.isIFBoxHost()
                            && firmware.modelName.equals(AppPreference.getIFBoxOTAInfo().modelName);
                    // プリペイドアプリ用
                    final boolean isPrepaid = firmware.modelName.equals(BuildConfig.PREPAID_APP_MODEL);

                    final Products product = isApk
                            ? Products.ThisApp
                            : isIFBox
                            ? Products.IFBox
                            : Products.None;

                    final String targetFilename = _localDir.toString() + File.separator + firmware.modelName + (isApk ? ".apk" : isPrepaid ? ".apk" : "");
                    final String tempFilename = targetFilename + ".download";
                    final StorageDownloadFileOptions  options = StorageDownloadFileOptions.defaultInstance();

                    _operation = Amplify.Storage.downloadFile(
                            firmware.binKey,
                            new File(tempFilename),
                            options,
                                progress -> {
                                Timber.d("Amplify.Storage.downloadFile.onProgress(): %s", progress);

                                // ダウンロード中
                                emitter.onNext(new DownloadProgress(
                                        false,
                                        false,
                                        (int) (progress.getFractionCompleted() * 100),
                                        null,
                                        product,
                                        fileNo,
                                        _updateList.size()));
                            },
                            result -> {
                                Timber.d("Amplify.Storage.downloadFile.onSuccess(): %s", result);

                                // ファイル名をリネームし、正式なものへ変更
                                new File(tempFilename).renameTo(new File(targetFilename));

                                try {
                                    File srcFile = new File(targetFilename);
                                    String fileName = srcFile.getName();
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        // ● Android10：MediaStore 経由で Download に登録
                                        ContentValues cv = new ContentValues();
                                        cv.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                                        cv.put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream");
                                        cv.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                                        Uri uri = _app.getContentResolver()
                                                .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                                        if (uri != null) {
                                            try (InputStream in = new FileInputStream(srcFile);
                                                 OutputStream out = _app.getContentResolver().openOutputStream(uri)) {
                                                byte[] buf = new byte[8 * 1024];
                                                int len;
                                                while ((len = in.read(buf)) > 0) {
                                                    out.write(buf, 0, len);
                                                }
                                            }
                                        }
                                    } else {
                                        // ● Android9 以下：直接ファイルコピー
                                        File destDir = Environment.getExternalStoragePublicDirectory(
                                                Environment.DIRECTORY_DOWNLOADS
                                        );
                                        if (!destDir.exists() && !destDir.mkdirs()) {
                                            throw new IOException("failed to create " + destDir);
                                        }
                                        Files.copy(
                                                srcFile.toPath(),
                                                new File(destDir, fileName).toPath(),
                                                StandardCopyOption.REPLACE_EXISTING
                                        );
                                    }
                                    Timber.d("Copied to public Download: %s", fileName);
                                } catch (Exception e) {
                                    Timber.e(e, "Failed to copy to public path");
                                }

                                Timber.d("binary path: %s", targetFilename);

                                firmware.downloadSuccessful = true;
                                if (fileNo >= _updateList.size()) {
                                    if (!emitter.isDisposed()) {
                                        emitter.onComplete();
                                    }
                                } else {
                                    this.run(i+1);
                                }
                            },
                            error -> {
                                Timber.e(error, "Amplify.Storage.downloadFile.onError()");
                                firmware.downloadSuccessful = false;
                                if (fileNo >= _updateList.size()) {
                                    if (!emitter.isDisposed()) {
                                        emitter.onError(new Throwable());
                                    }
                                } else {
                                    this.run(i+1);
                                }
                            }
                    );
                }
            };

            downloadTask.run(0);
        });
    }

    public Completable install() {
        return install(0, null);
    }

    public Completable install(int timeout, TimeUnit timeUnit) {
        return Completable.create(emitter -> {
            if (_updateList.size() <= 0) {
                if (!emitter.isDisposed()) {
                    emitter.onError(new IllegalStateException("update not exists"));
                }
                return;
            }

            FirmWareInfo _apkInfo = null;
            try {
                _apkInfo = Observable.fromIterable(_updateList)
                        .filter(f -> f.modelName.equals(_app.getPackageName()) && f.downloadSuccessful)
                        .blockingFirst();
            } catch (NoSuchElementException ignore) { }
            final FirmWareInfo apkInfo = _apkInfo;

            FirmWareInfo _ifBoxInfo = null;
            if (AppPreference.isIFBoxHost()) {
                try {
                    _ifBoxInfo = Observable.fromIterable(_updateList)
                            .filter(f -> f.modelName.equals(AppPreference.getIFBoxOTAInfo().modelName) && f.downloadSuccessful)
                            .blockingFirst();
                } catch (NoSuchElementException ignore) { }
            }
            final FirmWareInfo ifBoxInfo = _ifBoxInfo;

            // プリペイドアプリ用
            FirmWareInfo _prepaidInfo = null;
            if(true) {
                try {
                    _prepaidInfo = Observable.fromIterable(_updateList)
                            .filter(f -> f.modelName.equals(BuildConfig.PREPAID_APP_MODEL) && f.downloadSuccessful)
                            .blockingFirst();
                } catch (NoSuchElementException ignore) { }
            }
            final FirmWareInfo prepaidInfo = _prepaidInfo;

            if (ifBoxInfo != null) {
                installIFBox(ifBoxInfo, timeout, timeUnit)
                        .subscribeOn(Schedulers.io())
                        .timeout(timeout, TimeUnit.MINUTES)
                        .doFinally(() -> {
                            if (apkInfo != null) {
                                installApk();
                            }
                            if (!emitter.isDisposed()) {
                                emitter.onComplete();
                            }
                        })
                        .subscribe(() -> {
                        }, error -> {
                        });
            } else {
                if (prepaidInfo != null) {
                    installPrepaidApk(prepaidInfo);
                }

                if (apkInfo != null) {
                    installApk();
                }

                if (!emitter.isDisposed()) {
                    emitter.onComplete();
                }
            }
        });
    }

    private void installPrepaidApk(FirmWareInfo firmWareInfo) {
        PackageManager pm = _app.getPackageManager();
        String packageName = null;
        final File apkFile = new File(_localDir, firmWareInfo.modelName + ".apk");

        final Uri apkUri;
        final Intent install;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //新しい呼び方
            apkUri = FileProvider.getUriForFile(_app, BuildConfig.APPLICATION_ID + ".provider", apkFile);
            install = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            install.setData(apkUri);
            install.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            //従来
            apkUri = Uri.fromFile(apkFile);
            install = new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(apkUri, "application/vnd.android.package-archive");
            install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        _app.startActivity(install);
    }

    private void installApk() {
        File publicDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
        );
        final File apkFile = new File(
                publicDir,
                _app.getPackageName() + ".apk"
        );
        //インストールする

        final Uri apkUri;
        final Intent install;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //新しい呼び方
            apkUri = FileProvider.getUriForFile(_app, BuildConfig.APPLICATION_ID + ".provider", apkFile);
            String path = apkFile.getAbsolutePath();
            int result = SystemManager.installApp(path, SystemManager.MODE_AUTO_RUN_AFTER_SILENT_INSTALLED);
        } else {
            //従来
            apkUri = Uri.fromFile(apkFile);
            install = new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(apkUri, "application/vnd.android.package-archive");
            install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            _app.startActivity(install);
        }
    }
    private Completable installIFBox(FirmWareInfo firmware, int timeout, TimeUnit timeUnit) {
        return Completable.create(outer -> {
            final String targetFilename = _localDir.toString() + File.separator + firmware.modelName;

            Completable c = Completable.create(inner -> {
                DeviceServiceInfo ds;

                while (true) {
                    ds = _deviceNetworkManager.getDeviceServiceInfo().getValue();
                    if (ds.isAvailable()) {
                            if (!inner.isDisposed()) {
                                inner.onComplete();
                            }
                            break;
                    };

                    sleep(1000);
                }
            });

            if (timeout > 0 && timeUnit != null) {
                c.timeout(timeout, timeUnit);
            }

            c.subscribeOn(Schedulers.io()).subscribe(() -> {
                try {
                    DeviceServiceInfo ds;
                    ds = _deviceNetworkManager.getDeviceServiceInfo().getValue();

                    IFBoxApi api = new IFBoxApiImpl();
                    api.setBaseUrl("http://" + ds.getAddress());

                    api.postUpdate(targetFilename);

                    Completable.create(reconnectEmmiter -> {
                        // 再接続を待つ
                        AtomicBoolean isLost = new AtomicBoolean(false);
                        _deviceNetworkManager.getDeviceServiceInfo().subscribe(info -> {
                            /**/ if (info.getLost()) isLost.set(true);  // 切断
                            else if (isLost.get() && info.isAvailable()) {
                                api.setBaseUrl("http://" + info.getAddress());
                                reconnectEmmiter.onComplete();  // 切断後の再接続
                            }
                        });
                    }).subscribe(() -> {
                        AtomicReference<Version.Response> version = new AtomicReference<>(null);
                        Runnable versionCheck = () -> {
                            try {
                                Version.Response v = api.getVersion();
                                Timber.d("name: %s, model: %s, version: %s", v.appName, v.appModel, v.appVersion);

                                version.set(v);
                            } catch (Exception e) {
                                Timber.e(e);
                            }
                        };

                        // WiFi Direct接続直後は通信に失敗することがあるのでリトライする
                        for (int i = 0; i < 3; i++) {
                            versionCheck.run();
                            if (version.get() != null) break;
                            sleep(500);
                        }

                        if (version.get() == null) {
                            return;
                        }


                        AppPreference.setIFBoxOTAInfo(firmware);
//                        AppPreference.setIFBoxVersionInfo(version.get());

                        if (!outer.isDisposed()) {
                            outer.onComplete();
                        }
                        return;
                    }, e -> {
                        Timber.e(e);
                        if (!outer.isDisposed()) {
                            outer.onError(e);
                        }
                        return;
                    });
                } catch (Exception e) {
                    Timber.e(e);
                    if (!outer.isDisposed()) {
                        outer.onError(e);
                    }
                    return;
                }
            }, error -> {
                if (!outer.isDisposed()) {
                    outer.onError(new TimeoutException());
                }
                return;
            });
        });
    }

    private void sleep(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException ignore) { }
    }
}

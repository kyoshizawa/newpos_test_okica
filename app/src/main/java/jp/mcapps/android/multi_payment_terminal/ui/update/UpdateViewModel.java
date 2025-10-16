package jp.mcapps.android.multi_payment_terminal.ui.update;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.pos.device.sys.SystemManager;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jp.mcapps.android.multi_payment_terminal.BuildConfig;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.model.Updater;
import timber.log.Timber;

public class UpdateViewModel extends ViewModel {
    public enum Stages {
        Top,
        Checking,
        Confirm,
        Downloading,
        Installing,
        Finished,
    }

    private final Updater _updater;
    private final Handler _handler = new Handler(Looper.getMainLooper());

    public UpdateViewModel(Updater updater) {
        _updater = updater;
    }

    private final String _apkVersionCode = getVersionCode();
    public String getApkVersionCode() {
        return _apkVersionCode;
    }

    private final String _apkVersionName = getVersionName();
    public String getApkVersionName() {
        return _apkVersionName;
    }

    private MutableLiveData<String> _prepaidAppApkVersionCode = new MutableLiveData<>();
    public LiveData<String> getPrepaidAppApkVersionCode() {
        return _prepaidAppApkVersionCode;
    }
    public void SetPrepaidAppApkVersionCode() {
        _prepaidAppApkVersionCode.setValue(getPrepaidAppVersionCode());
    }

    private MutableLiveData<String> _prepaidAppApkVersionName = new MutableLiveData<>();
    public LiveData<String> getPrepaidAppApkVersionName() {
        return _prepaidAppApkVersionName;
    }
    public void SetPrepaidAppApkVersionName() {
        _prepaidAppApkVersionName.setValue(getPrepaidAppVersionName());
    }

    private final boolean _isPrepaid = getPrepaidEnabled();
    public Boolean getIsPrepaid() {
        return _isPrepaid;
    }

    private final MutableLiveData<Stages> _stage = new MutableLiveData<>(Stages.Top);
    public MutableLiveData<Stages> getStage() {
        return _stage;
    }
    public void setStage(Stages stage) {
        _handler.post(() -> _stage.setValue(stage));
    }

    private MutableLiveData<Boolean> _needReboot = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> needReboot() {
        return _needReboot;
    }
    public void needReboot(boolean b) {
        _handler.post(() -> _needReboot.setValue(b));
    }

    // 更新がない場合・失敗した場合に表示
    // 更新成功時はアプリの再起動がかかるので表示されない
    private MutableLiveData<String> _resultMessage = new MutableLiveData<>(null);
    public MutableLiveData<String> getResultMessage() {
        return _resultMessage;
    }
    public void setResultMessage(String message) {
        _handler.post(() -> _resultMessage.setValue(message));
    }

    private MutableLiveData<Integer> _progress = new MutableLiveData<>(0);
    public MutableLiveData<Integer> getProgress() {
        return _progress;
    }
    public void setProgress(int value) {
        _handler.post(() -> _progress.setValue(value));
    }

    private MutableLiveData<String> _downloadFileNo = new MutableLiveData<>("-");
    public MutableLiveData<String> getDownloadFileNo() {
        return _downloadFileNo;
    }
    public void setDownloadFileNo(String n) {
        _handler.post(() -> _downloadFileNo.setValue(n));
    }

    private MutableLiveData<String> _downloadTotalNum = new MutableLiveData<>("-");
    public MutableLiveData<String> getDownloadTotalNum() {
        return _downloadTotalNum;
    }
    public void setDownloadTotalNum(String n) {
        _handler.post(() -> _downloadTotalNum.setValue(n));
    }

    private boolean _isUpdateComplete = false;

    public void checkUpdateCancel() {
        // パッケージマネージャーの更新画面でキャンセルされた場合
        boolean isUpdateCancel = getStage().getValue() == UpdateViewModel.Stages.Finished
                && _isUpdateComplete
                && needReboot().getValue();

        if (isUpdateCancel) {
            setResultMessage("中断しました");
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void updateCheck(boolean isPreUpdate) {
        reset();
        _updater.getLatestVersion(isPreUpdate, false)
                .observeOn(Schedulers.io())
                .doOnSubscribe(d -> setStage(Stages.Checking))
                .subscribe((needReboot, error) -> {
                    if (error != null) {
                        if (error instanceof Updater.NewVersionNotExistsException) {
                            setResultMessage("更新がありません");
                        } else {
                            setResultMessage("更新チェックに失敗しました");
                        }
                        setStage(Stages.Finished);
                        return;
                    }

                    needReboot(needReboot);
                    setStage(Stages.Confirm);
                });
    };

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void prepaidAppUpdateCheck(boolean isPreUpdate) {
        reset();
        _updater.getPrepaidAppLatestVersion(isPreUpdate)
                .observeOn(Schedulers.io())
                .doOnSubscribe(d -> setStage(Stages.Checking))
                .subscribe((needReboot, error) -> {
                    if (error != null) {
                        if (error instanceof Updater.NewVersionNotExistsException) {
                            setResultMessage("更新がありません");
                        } else {
                            setResultMessage("更新チェックに失敗しました");
                        }
                        setStage(Stages.Finished);
                        return;
                    }

                    needReboot(needReboot);
                    setStage(Stages.Confirm);
                });
    };

    public void update() {
        _updater.download()
                .doOnSubscribe(d -> setStage(Stages.Downloading))
                .subscribe(downloadProgress -> {
                    // onNext
                    setDownloadFileNo(Integer.toString(downloadProgress.fileNo));
                    setDownloadTotalNum(Integer.toString(downloadProgress.totalFileNum));
                    setProgress(downloadProgress.progress);
                }, throwable -> {
                    // onError
                    Timber.e(throwable);
                    setResultMessage("ダウンロードに失敗しました");
                    setStage(Stages.Finished);
                }, () -> {
                    // onComplete
                    install();
                });
    }

    private void install() {
        _updater.install(1, TimeUnit.MINUTES)
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(d -> setStage(Stages.Installing))
                .doFinally(() -> {
                    setStage(Stages.Finished);
                })
                .subscribe(() -> {
                    if (!needReboot().getValue()) {
                        setResultMessage("更新完了しました");
                    }

                    _isUpdateComplete = true;
                }, error -> {
                    Timber.e(error);
                    setResultMessage("インストールに失敗しました");
                });
    }

    public void getPrepaidAppVersion() {
        _updater.getPrepaidAppVersion();
        SetPrepaidAppApkVersionCode();
        SetPrepaidAppApkVersionName();
    }

    private String getVersionCode() {
        return String.valueOf(BuildConfig.VERSION_CODE);
    }

    private String getVersionName() {
        try {
            PackageInfo packageInfo = MainApplication.getInstance().getPackageManager()
                    .getPackageInfo(MainApplication.getInstance().getPackageName(),
                            PackageManager.PackageInfoFlags.of(0));

            return "version: " + packageInfo.versionName;
        } catch (Exception e) {
            Timber.e(e);
            return "";
        }
    }

    private String getPrepaidAppVersionCode() {
        if (AppPreference.getPrepaidAppVersionCode() == null) {
            return null;
        }
        return AppPreference.getPrepaidAppVersionCode().toString();
    }

    private String getPrepaidAppVersionName() {
        return "version: " + AppPreference.getPrepaidAppVersionName();
    }

    private Boolean getPrepaidEnabled() {
        return AppPreference.getIsPrepaid();
    }

    //画面OFFの有効、無効を変更
    protected void setScreenTimeout(boolean isValid) {
        //有効時 -> センター設定値、無効時 -> 1日
        final int SCREEN_TIMEOUT_INVALID_SECOND = 60 * 60 * 24;
        int timeout = isValid ? AppPreference.getTimeoutScreen() : SCREEN_TIMEOUT_INVALID_SECOND;
        try {
            if (MainApplication.getInstance().isSDKInit()) {
                //別Activityに遷移するため、WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON は使わない
                SystemManager.setScreenTimeOut(timeout);
            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private void reset() {
        _isUpdateComplete = false;
        setProgress(0);
        setResultMessage(null);
        setDownloadFileNo("-");
        setDownloadTotalNum("-");
    }

    public String getBuildDate() {
        if (BuildConfig.DEBUG) {
            long buildTimeMillis = Long.parseLong(BuildConfig.BUILD_TIME);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
            return sdf.format(new Date(buildTimeMillis));
        } else {
            return " ";
        }
    }
}

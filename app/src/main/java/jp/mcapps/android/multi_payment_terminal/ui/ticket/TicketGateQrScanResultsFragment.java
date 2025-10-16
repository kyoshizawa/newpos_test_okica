package jp.mcapps.android.multi_payment_terminal.ui.ticket;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import java.io.File;

import jp.mcapps.android.multi_payment_terminal.AppPreference;
import jp.mcapps.android.multi_payment_terminal.BaseFragment;
import jp.mcapps.android.multi_payment_terminal.MainApplication;
import jp.mcapps.android.multi_payment_terminal.NavigationWrapper;
import jp.mcapps.android.multi_payment_terminal.R;
import jp.mcapps.android.multi_payment_terminal.ScreenData;
import jp.mcapps.android.multi_payment_terminal.databinding.FragmentTicketGateQrScanResultsBinding;
import jp.mcapps.android.multi_payment_terminal.model.SoundManager;
import jp.mcapps.android.multi_payment_terminal.util.CacheUtils;
import timber.log.Timber;


public class TicketGateQrScanResultsFragment extends BaseFragment implements TicketGateQrScanResultsHandlers {

    private final String SCREEN_NAME = "QR結果画面";
    private TicketGateQrScanResultsViewModel _ticketGateQrScanResultsViewModel;
    private TicketGateQrScanResults _ticketGateQrScanResults;
    private SoundManager _soundManager = SoundManager.getInstance();

    public static TicketGateQrScanResultsFragment newInstance() {
        return new TicketGateQrScanResultsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        final FragmentTicketGateQrScanResultsBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_ticket_gate_qr_scan_results, container, false);

        _ticketGateQrScanResultsViewModel = new ViewModelProvider(this).get(TicketGateQrScanResultsViewModel.class);

        binding.setViewModel(_ticketGateQrScanResultsViewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setHandlers(this);

        final Bundle args = getArguments();
        _ticketGateQrScanResults = new TicketGateQrScanResults();
        _ticketGateQrScanResults = args.getSerializable("QrScanResults", TicketGateQrScanResults.class);

        qrScanResultsView();
        qrScanResultsSound();

        ScreenData.getInstance().setScreenName(SCREEN_NAME);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        popBackStackToQrScan(view);
    }

    private void qrScanResultsView() {

        _ticketGateQrScanResultsViewModel.setResult(_ticketGateQrScanResults.qrScanResult);
        if (_ticketGateQrScanResults.qrScanResult) {
            /* 確認結果：成功 */
            _ticketGateQrScanResultsViewModel.setAdultNumber(_ticketGateQrScanResults.adultNumber);
            _ticketGateQrScanResultsViewModel.setChildNumber(_ticketGateQrScanResults.childNumber);
            _ticketGateQrScanResultsViewModel.setBabyNumber(_ticketGateQrScanResults.babyNumber);
            _ticketGateQrScanResultsViewModel.setAdultDisabilityNumber(_ticketGateQrScanResults.adultDisabilityNumber);
            _ticketGateQrScanResultsViewModel.setChildDisabilityNumber(_ticketGateQrScanResults.childDisabilityNumber);
            _ticketGateQrScanResultsViewModel.setCaregiverNumber(_ticketGateQrScanResults.caregiverNumber);
            _ticketGateQrScanResultsViewModel.setTotalPeoples(_ticketGateQrScanResults.totalPeoples);
            Timber.i("QR確認結果成功: 合計人数=%s 大人=%s 小人=%s 乳幼児=%s 障がい者(大人)=%s 障がい者(小人)=%s 介助者=%s",
                    _ticketGateQrScanResults.totalPeoples, _ticketGateQrScanResults.adultNumber, _ticketGateQrScanResults.childNumber, _ticketGateQrScanResults.babyNumber,
                    _ticketGateQrScanResults.adultDisabilityNumber, _ticketGateQrScanResults.childDisabilityNumber, _ticketGateQrScanResults.caregiverNumber);
        } else {
            /* 確認結果：失敗 */
            _ticketGateQrScanResultsViewModel.setErrorCode(_ticketGateQrScanResults.errorCode);
            _ticketGateQrScanResultsViewModel.setErrorMessage(_ticketGateQrScanResults.errorMessage);
            _ticketGateQrScanResultsViewModel.setErrorMessageEnglish(_ticketGateQrScanResults.errorMessageEnglish);
            Timber.i("QR確認結果失敗: エラーコード=[%s] エラーメッセージ=[%s] 英語メッセージ=[%s]",
                    _ticketGateQrScanResults.errorCode, _ticketGateQrScanResults.errorMessage, _ticketGateQrScanResults.errorMessageEnglish);
        }
    }

    private void popBackStackToQrScan(View view) {

        long waitingTimeMillisecond = 4000; // 成功時：4秒
        if (!_ticketGateQrScanResults.qrScanResult) waitingTimeMillisecond = 3000; // 失敗時：3秒
        CountDownTimer countDownTimer = new CountDownTimer(waitingTimeMillisecond, 1000) {
            @Override
            public void onTick(long l) {

            }

            @Override
            public void onFinish() {
                // QR画像で200KB程度使用するので終わったらキャッシュをクリアする
                CacheUtils.clearCache(getContext());

                // QRかざし待ち画面に戻す
                NavigationWrapper.popBackStack(view);
            }
        }.start();
    }

    public void qrScanResultsSound() {
        if (_ticketGateQrScanResults.qrScanResult) {
            // チケット確認結果がOKの場合
            if (null == _ticketGateQrScanResults.totalPeoples) {
                // その他（想定外）
                soundPlay(R.raw.se_sod05);
            } else if (1 == _ticketGateQrScanResults.totalPeoples) {
                // 一人の場合
                if ((null != _ticketGateQrScanResults.adultNumber && 1 == _ticketGateQrScanResults.adultNumber)
                        || (null != _ticketGateQrScanResults.adultDisabilityNumber && 1 == _ticketGateQrScanResults.adultDisabilityNumber)) {
                    // 大人 or 大人障がい者
                    soundPlay(R.raw.se_soc02);
                } else if ((null != _ticketGateQrScanResults.childNumber && 1 == _ticketGateQrScanResults.childNumber)
                        || (null != _ticketGateQrScanResults.childDisabilityNumber && 1 == _ticketGateQrScanResults.childDisabilityNumber)) {
                    // 小人 or 小人障がい者
                    soundPlay(R.raw.se_mod01);
                } else {
                    // その他（想定外）
                    soundPlay(R.raw.se_sod05);
                }
            } else {
                // 複数人の場合
                soundPlay(R.raw.se_soc05);
            }
        } else {
            // チケット確認結果がNGの場合
            soundPlay(R.raw.se_sod05);
        }
    }

    public void soundPlay(@RawRes int id) {
        float volume = 0f;
        _soundManager.load(MainApplication.getInstance(), id, 1);
        volume =  AppPreference.getSoundGuidanceVolume() / 10f;

        float leftVolume = volume;
        float rightVolume = volume;
        _soundManager.setOnLoadCompleteListener((soundPool, soundId, status) -> {
            soundPool.play(soundId, leftVolume, rightVolume, 1, 0, 1);
        });
    }
}

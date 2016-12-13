package dynoapps.exchange_rates.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import dynoapps.exchange_rates.LandingActivity;
import dynoapps.exchange_rates.R;
import dynoapps.exchange_rates.SourcesManager;
import dynoapps.exchange_rates.alarm.Alarm;
import dynoapps.exchange_rates.alarm.AlarmManager;
import dynoapps.exchange_rates.alarm.AlarmsHolder;
import dynoapps.exchange_rates.data.CurrencySource;
import dynoapps.exchange_rates.data.RatesHolder;
import dynoapps.exchange_rates.event.DataSourceUpdate;
import dynoapps.exchange_rates.event.IntervalUpdate;
import dynoapps.exchange_rates.event.RatesEvent;
import dynoapps.exchange_rates.model.rates.AvgRate;
import dynoapps.exchange_rates.model.rates.BaseRate;
import dynoapps.exchange_rates.model.rates.BigparaRate;
import dynoapps.exchange_rates.model.rates.BuySellRate;
import dynoapps.exchange_rates.model.rates.DolarTlKurRate;
import dynoapps.exchange_rates.model.rates.EnparaRate;
import dynoapps.exchange_rates.model.rates.YahooRate;
import dynoapps.exchange_rates.model.rates.YapıKrediRate;
import dynoapps.exchange_rates.model.rates.YorumlarRate;
import dynoapps.exchange_rates.provider.BasePoolingDataProvider;
import dynoapps.exchange_rates.provider.BigparaRateProvider;
import dynoapps.exchange_rates.provider.DolarTlKurRateProvider;
import dynoapps.exchange_rates.provider.EnparaRateProvider;
import dynoapps.exchange_rates.provider.IPollingSource;
import dynoapps.exchange_rates.provider.ProviderSourceCallbackAdapter;
import dynoapps.exchange_rates.provider.YahooRateProvider;
import dynoapps.exchange_rates.provider.YapıKrediRateProvider;
import dynoapps.exchange_rates.provider.YorumlarRateProvider;
import dynoapps.exchange_rates.util.Formatter;
import dynoapps.exchange_rates.util.RateUtils;

/**
 * Created by erdemmac on 05/12/2016.
 */

public class RatePollingService extends Service {
    ArrayList<BasePoolingDataProvider> providers = new ArrayList<>();

    private final IBinder mBinder = new SimpleBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
        if (providers.size() > 0) return;
        providers.add(new YorumlarRateProvider(new ProviderSourceCallbackAdapter<List<YorumlarRate>>() {
            @Override
            public void onResult(List<YorumlarRate> rates) {
                alarmChecks(rates, CurrencySource.Type.YORUMLAR);
                RatesHolder.getInstance().addRate(rates, CurrencySource.Type.YORUMLAR);
                EventBus.getDefault().post(new RatesEvent<>(rates, CurrencySource.Type.YORUMLAR));
            }
        }));
        providers.add(new EnparaRateProvider(new ProviderSourceCallbackAdapter<List<EnparaRate>>() {
            @Override
            public void onResult(List<EnparaRate> rates) {
                alarmChecks(rates, CurrencySource.Type.ENPARA);
                RatesHolder.getInstance().addRate(rates, CurrencySource.Type.ENPARA);
                EventBus.getDefault().post(new RatesEvent<>(rates, CurrencySource.Type.ENPARA, System.currentTimeMillis()));
            }
        }));

        providers.add(
                new BigparaRateProvider(new ProviderSourceCallbackAdapter<List<BigparaRate>>() {
                    @Override
                    public void onResult(List<BigparaRate> rates) {
                        alarmChecks(rates, CurrencySource.Type.BIGPARA);
                        RatesHolder.getInstance().addRate(rates, CurrencySource.Type.BIGPARA);
                        EventBus.getDefault().post(new RatesEvent<>(rates, CurrencySource.Type.BIGPARA));
                    }
                }));

        providers.add(new DolarTlKurRateProvider(new ProviderSourceCallbackAdapter<List<DolarTlKurRate>>() {
            @Override
            public void onResult(List<DolarTlKurRate> rates) {
                alarmChecks(rates, CurrencySource.Type.TLKUR);
                RatesHolder.getInstance().addRate(rates, CurrencySource.Type.TLKUR);
                EventBus.getDefault().post(new RatesEvent<>(rates, CurrencySource.Type.TLKUR));
            }
        }));


        providers.add(new YapıKrediRateProvider(new ProviderSourceCallbackAdapter<List<YapıKrediRate>>() {
            @Override
            public void onResult(List<YapıKrediRate> rates) {
                alarmChecks(rates, CurrencySource.Type.YAPIKREDI);
                RatesHolder.getInstance().addRate(rates, CurrencySource.Type.YAPIKREDI);
                EventBus.getDefault().post(new RatesEvent<>(rates, CurrencySource.Type.YAPIKREDI));
            }
        }));
        providers.add(new YahooRateProvider(new ProviderSourceCallbackAdapter<List<YahooRate>>() {
            @Override
            public void onResult(List<YahooRate> rates) {
                alarmChecks(rates, CurrencySource.Type.YAHOO);
                RatesHolder.getInstance().addRate(rates, CurrencySource.Type.YAHOO);
                EventBus.getDefault().post(new RatesEvent<>(rates, CurrencySource.Type.YAHOO));
            }
        }));
        SourcesManager.init();
        SourcesManager.updateProviders(providers);
        refreshSources();
    }

    private static Formatter formatter = new Formatter(3);

    private <T extends BaseRate> void alarmChecks(List<T> rates, int source_type) {
        if (rates == null) return;
        AlarmsHolder alarmsHolder = AlarmManager.getAlarmsHolder();
        Iterator<Alarm> iterator = alarmsHolder.alarms.iterator();
        int size = alarmsHolder.alarms.size();
        while (iterator.hasNext()) {
            Alarm alarm = iterator.next();
            if (alarm.source_type != source_type) continue;
            BaseRate baseRateCurrent = RateUtils.getRate(rates, alarm.rate_type);
            RatesEvent ratesEvent = RatesHolder.getInstance().getRates(source_type);
            BaseRate baseRateOld = ratesEvent != null ? RateUtils.getRate(ratesEvent.rates, alarm.rate_type) : null;

            if (baseRateCurrent == null || baseRateOld == null) continue;
            float val_current = 0.0f;
            float val_old = 0.0f;
            if (baseRateCurrent instanceof AvgRate) {
                val_current = ((AvgRate) baseRateCurrent).avg_val_real;
                val_old = ((AvgRate) baseRateOld).avg_val_real;
            } else if (baseRateCurrent instanceof BuySellRate) {
                val_current = ((BuySellRate) baseRateCurrent).value_sell_real;
                val_old = ((BuySellRate) baseRateOld).value_sell_real;
            }
            if (alarm.is_above && val_current > alarm.val && val_old <= alarm.val) {
                iterator.remove();
                sendNotification(getString(R.string.is_above_val, formatter.format(alarm.val)), "increasing");
            } else if (!alarm.is_above && val_current < alarm.val && val_old >= alarm.val) {
                iterator.remove();
                sendNotification(getString(R.string.is_below_value, formatter.format(alarm.val)), "decreasing");
            }
        }
        if (size!=alarmsHolder.alarms.size()){
            AlarmManager.saveAlarms();
        }
    }

    @Subscribe
    public void onEvent(DataSourceUpdate event) {
        refreshSources();
    }

    @Subscribe
    public void onEvent(IntervalUpdate event) {
        for (BasePoolingDataProvider provider : providers) {
            provider.refreshForIntervals();
        }
    }

    private void refreshSources() {
        ArrayList<CurrencySource> currencySources = SourcesManager.getCurrencySources();
        for (CurrencySource currencySource : currencySources) {
            IPollingSource iPollingSource = currencySource.getPollingSource();
            if (currencySource.isEnabled()) {
                iPollingSource.start();
            } else {
                iPollingSource.stop();
            }
        }
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        if (providers != null) {
            for (IPollingSource iPollingSource : providers) {
                iPollingSource.stop();
            }
        }
        super.onDestroy();
    }


    private void sendNotification(String message, String category) {

        Intent pushIntent = new Intent(this, LandingActivity.class);
        pushIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                pushIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_add_alarm_white_24dp)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                        .setContentTitle(getString(R.string.app_name))
                        .setDefaults(Notification.FLAG_AUTO_CANCEL | Notification.DEFAULT_SOUND
                                | Notification.DEFAULT_VIBRATE | Notification.FLAG_SHOW_LIGHTS);


        mBuilder.setStyle(new NotificationCompat.BigTextStyle()
                .bigText(message));

        mBuilder.setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setContentIntent(pendingIntent)
                .setContentText(message);
        mBuilder.setAutoCancel(true);
        Notification notification = mBuilder.build();
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(category, 1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    public class SimpleBinder extends Binder {
        public RatePollingService getService() {
            return RatePollingService.this;
        }
    }
}

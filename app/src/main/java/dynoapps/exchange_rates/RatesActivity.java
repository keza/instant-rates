package dynoapps.exchange_rates;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.DefaultAxisValueFormatter;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IDataSet;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import dynoapps.exchange_rates.data.RateDataSource;
import dynoapps.exchange_rates.data.RatesHolder;
import dynoapps.exchange_rates.event.RatesEvent;
import dynoapps.exchange_rates.model.rates.BaseRate;
import dynoapps.exchange_rates.model.rates.BigparaRate;
import dynoapps.exchange_rates.model.rates.BuySellRate;
import dynoapps.exchange_rates.model.rates.DolarTlKurRate;
import dynoapps.exchange_rates.model.rates.EnparaRate;
import dynoapps.exchange_rates.model.rates.IRate;
import dynoapps.exchange_rates.model.rates.YapıKrediRate;
import dynoapps.exchange_rates.model.rates.YorumlarRate;
import dynoapps.exchange_rates.time.TimeIntervalManager;
import dynoapps.exchange_rates.util.RateUtils;
import dynoapps.exchange_rates.util.ViewUtils;

/**
 * Created by erdemmac on 24/11/2016.
 */

public class RatesActivity extends BaseActivity {

    private static final int DEFAULT_VISIBLE_CHART_SECONDS = 120; // 2 mins
    private static final float THRESHOLD_ERROR_USD_TRY = 0.2f;

    @BindView(R.id.line_usd_chart)
    LineChart usdLineChart;

    @BindView(R.id.v_progress_wheel)
    View vProgress;

    private long startMilis;
    SimpleDateFormat hourFormatter = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private int white;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
        getActionBarToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        white = ContextCompat.getColor(getApplicationContext(), android.R.color.white);
        startMilis = System.currentTimeMillis();
        initUsdChart();

        vProgress.setVisibility(View.GONE);

        SparseArray<RatesEvent<BaseRate>> sparseArray = RatesHolder.getInstance().getAllRates();
        if (sparseArray != null) {
            for (int i = 0; i < sparseArray.size(); i++) {
                RatesEvent<BaseRate> ratesEvent = sparseArray.valueAt(i);
                RateDataSource rateDataSource = DataSourcesManager.getSource(ratesEvent.sourceType);
                if (rateDataSource != null && rateDataSource.isEnabled()) {
                    update(ratesEvent.rates, ratesEvent.fetchTime);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    private void update(List<BaseRate> rates, long fetchMilis) {
        BaseRate rateUsd = RateUtils.getRate(rates, IRate.USD);
        if (rateUsd != null) {
            if (rateUsd instanceof YapıKrediRate) {
                addEntry(((YapıKrediRate) rateUsd).value_sell_real, 5, fetchMilis);
            } else if (rateUsd instanceof DolarTlKurRate) {
                addEntry(((DolarTlKurRate) rateUsd).realValue, 4, fetchMilis);
            } else if (rateUsd instanceof YorumlarRate) {
                addEntry(((YorumlarRate) rateUsd).realValue, 0, fetchMilis);
            } else if (rateUsd instanceof EnparaRate) {
                addEntry(((EnparaRate) rateUsd).value_sell_real, 1, fetchMilis);
                addEntry(((EnparaRate) rateUsd).value_buy_real, 2, fetchMilis);
            } else if (rateUsd instanceof BigparaRate) {
                addEntry(((BuySellRate) rateUsd).value_sell_real, 3, fetchMilis);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(RatesEvent ratesEvent) {
        List<BaseRate> rates = ratesEvent.rates;
        update(rates, ratesEvent.fetchTime);
    }


    private void initUsdChart() {
//        Description description = new Description();
//        description.setTextSize(12f);
//        description.setText("Dolar-TL Grafiği");
//        description.setXOffset(8);
//        description.setYOffset(8);
//        description.setTextColor(ContextCompat.getColor(this, android.R.color.white));
//        usdLineChart.setDescription(description);
        usdLineChart.getDescription().setEnabled(false);
        usdLineChart.setBackgroundColor(ContextCompat.getColor(this, R.color.colorGraph));

        // add an empty data object
        usdLineChart.setData(new LineData());
//        mChart.getXAxis().setDrawLabels(false);
        usdLineChart.getXAxis().setDrawGridLines(true);

        usdLineChart.getXAxis().setLabelCount(6);
//        usdLineChart.getAxisRight().setAxisMaximum(3.48f);
//        usdLineChart.getAxisRight().setAxisMinimum(3.42f);
        usdLineChart.getAxisLeft().setEnabled(false);
        usdLineChart.getXAxis().setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                Calendar calendar = Calendar.getInstance();
                int time = (int) value;
                calendar.add(Calendar.SECOND, time);
                Date date = calendar.getTime();

                return time > 0 ? hourFormatter.format(date) : "";
            }

        });

        final IAxisValueFormatter axisValueFormatter = new DefaultAxisValueFormatter(3);
        usdLineChart.getAxisRight().setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return axisValueFormatter.getFormattedValue(value, axis) + " TL";
            }
        });
        usdLineChart.setScaleEnabled(false);
        usdLineChart.invalidate();

        usdLineChart.setExtraBottomOffset(12);
        usdLineChart.setExtraTopOffset(12);
        usdLineChart.setPinchZoom(false);

        LineData data = usdLineChart.getData();
        data.addDataSet(createDataSet(0));
        data.addDataSet(createDataSet(1));
        data.addDataSet(createDataSet(2));
        data.addDataSet(createDataSet(3));
        data.addDataSet(createDataSet(4));
        data.addDataSet(createDataSet(5));

        Legend legend = usdLineChart.getLegend();
        legend.setTextSize(13);
        legend.setTextColor(white);
        legend.setYOffset(6);
        legend.setForm(Legend.LegendForm.CIRCLE);
        legend.setWordWrapEnabled(true);
        legend.setXEntrySpace(10);

        usdLineChart.setHighlightPerTapEnabled(true);
        CustomMarkerView customMarkerView = new CustomMarkerView(this, R.layout.view_marker);
        customMarkerView.setOffset(ViewUtils.dpToPx(4), -customMarkerView.getMeasuredHeight() - ViewUtils.dpToPx(4));
        usdLineChart.setMarker(customMarkerView);

        usdLineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        usdLineChart.getXAxis().setTextColor(white);
        usdLineChart.getAxisRight().setTextColor(white);

    }

    @SuppressLint("ViewConstructor")
    static class CustomMarkerView extends MarkerView {

        private TextView tvMarker;

        public CustomMarkerView(Context context, int layoutResource) {
            super(context, layoutResource);
            tvMarker = (TextView) findViewById(R.id.tv_marker);
        }

        // callbacks everytime the MarkerView is redrawn, can be used to update the
        // content (user-interface)
        @Override
        public void refreshContent(Entry e, Highlight highlight) {
            tvMarker.setText(App.context().getString(R.string.placeholder_tl, "" + e.getY())); // set the entry-value as the display text
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_rates, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_item_sources) {
            DataSourcesManager.selectSources(this);
            return true;
        } else if (id == R.id.menu_time_interval) {
            TimeIntervalManager.selectInterval(this);
            return true;
        } else if (id == R.id.menu_item_refresh) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(true);
            builder.setTitle(R.string.refresh);
            builder.setMessage(R.string.clear_sure_message);
            builder.setPositiveButton(R.string.sure, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    for (int i = 0; i < usdLineChart.getData().getDataSetCount(); i++) {
                        IDataSet iDataSet = usdLineChart.getData().getDataSetByIndex(i);
                        iDataSet.clear();

                    }
                    usdLineChart.getXAxis().resetAxisMaximum();
                    usdLineChart.invalidate();
                    usdLineChart.notifyDataSetChanged();
                    startMilis = System.currentTimeMillis();
                    usdLineChart.moveViewToX(0);
                }
            });

            builder.setNegativeButton(R.string.dismiss, null);
            AlertDialog dialog = builder.create();
            dialog.show();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void addEntry(float value, int chartIndex, long milis) {
        if (THRESHOLD_ERROR_USD_TRY > value) return;
        LineData data = usdLineChart.getData();
        int newX = (int) (((milis - startMilis) / 1000));

        Entry entry = new Entry(newX, value);
        data.addEntry(entry, chartIndex);
        data.notifyDataChanged();
        IDataSet dataSet = data.getDataSetByIndex(chartIndex);
        if (Math.abs(dataSet.getXMin() - dataSet.getXMax()) > DEFAULT_VISIBLE_CHART_SECONDS * 2 && dataSet.getEntryCount() > DEFAULT_VISIBLE_CHART_SECONDS) {
            dataSet.removeEntry(0);
        }

        // let the chart know it's data has changed
        usdLineChart.notifyDataSetChanged();

        //mChart.setVisibleYRangeMaximum(15, AxisDependency.LEFT);
        usdLineChart.setVisibleXRangeMaximum(DEFAULT_VISIBLE_CHART_SECONDS);

        if (usdLineChart.getXAxis().getAxisMaximum() <= newX) {
            usdLineChart.moveViewToX(newX);
        } else if (usdLineChart.getVisibleXRange() < newX) {
            usdLineChart.moveViewToX(newX + usdLineChart.getVisibleXRange());
        } else {
            usdLineChart.invalidate();
        }
    }

    private LineDataSet createDataSet(int chartIndex) {

        String label;
        switch (chartIndex) {
            case 0:
                label = "yorumlar.altin.in";
                break;
            case 1:
                label = "Enpara Satış";
                break;
            case 2:
                label = "Enpara Alış";
                break;
            case 3:
                label = "Bigpara";
                break;
            case 4:
                label = "dolar.tlkur.com";
                break;
            case 5:
                label = "Yapı Kredi";
                break;
            default:
                label = "Unknown";
                break;
        }

        LineDataSet set = new LineDataSet(null, label);
        set.setCubicIntensity(0.1f);
        set.setDrawCircleHole(false);
        set.setLineWidth(1.5f);
        set.setCircleRadius(2f);
        set.setDrawCircles(true);
        int color;
        if (chartIndex == 0) {
            color = ContextCompat.getColor(this, R.color.colorYorumlar);
        } else if (chartIndex == 1) {
            color = ContextCompat.getColor(this, R.color.colorEnpara);
        } else if (chartIndex == 2) {
            color = ContextCompat.getColor(this, R.color.colorEnpara);
        } else if (chartIndex == 3) {
            color = ContextCompat.getColor(this, R.color.colorBigPara);
        } else if (chartIndex == 4) {
            color = ContextCompat.getColor(this, R.color.colorDolarTlKur);
        } else if (chartIndex == 5) {
            color = ContextCompat.getColor(this, R.color.colorYapıKredi);
        } else {
            color = ContextCompat.getColor(this, R.color.colorBigPara);
        }


        set.setCircleColor(color);
        set.setHighLightColor(Color.rgb(155, 155, 155));
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(color);
//        set.setDrawFilled(true);
        set.setFillAlpha((int) (256 * 0.3f));
        set.setFillColor(color);
        set.setValueTextColor(color);
        set.setValueTextSize(16f);
        set.setDrawValues(false);
        return set;
    }


    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_rates;
    }


}



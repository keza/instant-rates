package dynoapps.exchange_rates.alarm;

import java.io.Serializable;

import dynoapps.exchange_rates.interfaces.ValueType;

/**
 * Created by erdemmac on 13/12/2016.
 */

public class Alarm implements Serializable {
    public Float val;
    public boolean is_above = false;
    public boolean is_enabled = true;

    /**
     * {@link dynoapps.exchange_rates.model.rates.IRate}
     */
    public int rate_type;

    /**
     * {@link dynoapps.exchange_rates.data.CurrencyType}
     */
    public int source_type;

    /**
     * {@link dynoapps.exchange_rates.interfaces.ValueType}
     */
    public int value_type= ValueType.NONE;

    public static int getPushId(Alarm alarm) {
        return alarm.rate_type * 100 + alarm.source_type;
    }
}

package demoapps.exchange_rates.service;

import java.util.List;

import demoapps.exchange_rates.data.BuySellRate;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;

/**
 * Created by erdemmac on 24/10/2016.
 */

public interface EnparaService {

    @Headers({
            "Content-Type:text/html; charset=utf-8"
    })
    @GET("doviz-kur-bilgileri/doviz-altin-kurlari.aspx")
    Call<List<BuySellRate>> getValues();

}
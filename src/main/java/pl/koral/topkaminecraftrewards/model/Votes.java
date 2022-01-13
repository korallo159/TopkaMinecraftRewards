package pl.koral.topkaminecraftrewards.model;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;

public class Votes {

    @SerializedName(value = "likesHistory")
    private HashMap<Long, String> unsafe;

    private final transient HashMap<String, Long> likesHistory = new HashMap<>();

    public HashMap<String, Long> getLikesHistory() {
        this.unsafe.forEach((k, v) -> likesHistory.put(v, k));
        return likesHistory;
    }


    public Votes fetchData(long id, int days){
        try {
            URL url = new URL("https://api.jbwm.pl/api/serverlist/servers/likehistory/" + id + "?days=" + days);
            InputStreamReader reader = new InputStreamReader(url.openStream());

            return new Gson().fromJson(reader, Votes.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }
}

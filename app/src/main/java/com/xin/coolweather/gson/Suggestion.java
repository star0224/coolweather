package com.xin.coolweather.gson;

import com.google.gson.annotations.SerializedName;

public class Suggestion {

    @SerializedName("comf")
    public Comfort comfort;

    @SerializedName("cw")
    public CarWash carWash;

    public Sprot sport;

    public class Comfort {
        @SerializedName("txt")
        public String info;
    }

    public class CarWash {
        @SerializedName("txt")
        public String info;
    }

    public class Sprot {
        @SerializedName("txt")
        public String info;
    }
}

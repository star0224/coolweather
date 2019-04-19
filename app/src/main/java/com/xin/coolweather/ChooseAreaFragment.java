package com.xin.coolweather;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.xin.coolweather.db.City;
import com.xin.coolweather.db.Country;
import com.xin.coolweather.db.Province;
import com.xin.coolweather.util.HttpUtil;
import com.xin.coolweather.util.Utility;

import org.litepal.LitePal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;

    public static final int LEVEL_CITY = 1;

    public static final int LEVEL_COUNTRY = 2;

    private static final String TAG = "ChooseAreaFragment";

    private TextView mTitleText;

    private Button mBackButton;

    private ProgressDialog mProgressDialog;

    private ListView mListView;

    private ArrayAdapter<String> adapter;

    private List<String> dataList = new ArrayList<>();

    private List<City> cities;

    private List<Province> provinces;

    private List<Country> countries;

    private Province selectedProvince;

    private City selectedCity;

    private int currentLevel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        mTitleText = (TextView) view.findViewById(R.id.title_text);
        mBackButton = (Button) view.findViewById(R.id.back_btn);
        mListView = (ListView) view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        mListView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = provinces.get(position);
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    selectedCity = cities.get(position);
                    queryCountries();
                } else if (currentLevel == LEVEL_COUNTRY) {
                    String weatherId = countries.get(position).getWeatherId();
                    Intent intent = new Intent(getActivity(), WeatherActivity.class);
                    intent.putExtra("weather_id", weatherId);
                    startActivity(intent);
                    getActivity().finish();
                }
            }
        });

        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLevel == LEVEL_COUNTRY) {
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }

    private void queryProvinces() {
        mTitleText.setText("中国");
        mBackButton.setVisibility(View.GONE);
        provinces = LitePal.findAll(Province.class);
        if (provinces.size() > 0) {
            dataList.clear();
            for (Province province : provinces) {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            mListView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        } else {
            String address = "http://guolin.tech/api/china";
            queryFromServer(address, "province");
        }
    }

    private void queryCities() {
        mTitleText.setText(selectedProvince.getProvinceName());
        mBackButton.setVisibility(View.VISIBLE);
        cities = LitePal.where("provinceid=?", String.valueOf(selectedProvince.getId())).find(City.class);
        if (cities.size() > 0) {
            dataList.clear();
            for (City city : cities) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            mListView.setSelection(0);
            currentLevel = LEVEL_CITY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china" + "/" + provinceCode;
            queryFromServer(address, "city");
        }
    }

    private void queryCountries() {
        mTitleText.setText(selectedCity.getCityName());
        mBackButton.setVisibility(View.VISIBLE);
        countries = LitePal.where("cityid = ?", String.valueOf(selectedCity.getId())).find(Country.class);
        if (countries.size() > 0) {
            dataList.clear();
            for (Country country : countries) {
                dataList.add(country.getCountryName());
            }
            adapter.notifyDataSetChanged();
            mListView.setSelection(0);
            currentLevel = LEVEL_COUNTRY;
        } else {
            int provinceCoed = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china" + "/" + provinceCoed + "/" + cityCode;
            queryFromServer(address, "country");
        }
    }

    private void queryFromServer(String address, final String type) {
        showProgressBar();

        HttpUtil.sendRequest(address, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if ("province".equals(type)) {
                    result = Utility.handleProvinceResponse(responseText);
                } else if ("city".equals(type)) {
                    result = Utility.handleCityResponse(responseText, selectedProvince.getId());
                } else if ("country".equals(type)) {
                    result = Utility.handleCountryResponse(responseText, selectedCity.getId());
                }
                if (result) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressBar();
                            if ("province".equals(type)) {
                                queryProvinces();
                            } else if ("city".equals(type)) {
                                queryCities();
                            } else if ("country".equals(type)) {
                                queryCountries();
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressBar();
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void showProgressBar() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setMessage("正在加载...");
            mProgressDialog.setCanceledOnTouchOutside(false);
        }
        mProgressDialog.show();
    }

    private void closeProgressBar() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }
}

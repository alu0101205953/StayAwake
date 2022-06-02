package com.iris.StayAwake;

import com.robinhood.spark.SparkAdapter;

public class RandomizedAdapter extends SparkAdapter {
    private int[] yData;

    public RandomizedAdapter(int[] data) {
        yData = data;
    }

    @Override
    public int getCount() {
        return yData.length;
    }

    @Override
    public Object getItem(int index) {
        return yData[index];
    }

    @Override
    public float getY(int index)
    {
        return yData[index];
    }
}

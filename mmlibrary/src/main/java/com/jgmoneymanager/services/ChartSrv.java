package com.jgmoneymanager.services;

import android.graphics.Color;
import android.util.Log;

import com.jgmoneymanager.entity.CheckBoxItem;

import org.achartengine.chart.PointStyle;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by User on 24/04/2015.
 */
public class ChartSrv {

    public static int[] generateRandomColorsList(int size) {
        int[] colors = new int[size];
        Random rnd = new Random();
        for (int i=0; i<size; i++) {
            colors[i] = Color.argb(255, 150+rnd.nextInt(106), 150+rnd.nextInt(106), 150+rnd.nextInt(106));
        }
        return colors;
    }

    public static PointStyle[] generateRandomStyleList(int size) {
        PointStyle[] styles = new PointStyle[size];
        Random rnd = new Random();
        for (int i=0; i<size; i++) {
            int newStyle = rnd.nextInt(6);
            switch (newStyle) {
                case 0: styles[i] = PointStyle.X;
                case 1: styles[i] = PointStyle.CIRCLE;
                case 2: styles[i] = PointStyle.TRIANGLE;
                case 3: styles[i] = PointStyle.SQUARE;
                case 4: styles[i] = PointStyle.DIAMOND;
                case 5: styles[i] = PointStyle.POINT;
            }
        }
        return styles;
    }

    public static String convertBoolListToString(boolean[] checkedItemsList, ArrayList<CheckBoxItem> allItemsList) {
        String result = "";
        for (int i=0; i<checkedItemsList.length; i++) {
            if (checkedItemsList[i]) {
                CheckBoxItem item = allItemsList.get(i);
                result += item.getID() + ",";
                /*result.concat(String.valueOf(i));
                result.concat(",");*/
            }
        }
        Log.i("ListValue", result);
        return result;
    }

    public static boolean[] convertStringToBoolList(String listValue, ArrayList<CheckBoxItem> allItemsList) {
        boolean[] chedkedItemList = new boolean[allItemsList.size()];
        while (listValue.indexOf(",") != -1) {
            int currentIndex = listValue.indexOf(",");
            Integer curID = Integer.parseInt(listValue.substring(0, currentIndex));
            listValue = listValue.substring(currentIndex+1);
            for (int i = 0; i < allItemsList.size(); i++) {
                CheckBoxItem item = allItemsList.get(i);
                if (item.getID() == curID) {
                    chedkedItemList[i] = true;
                    break;
                }
            }
        }
        return chedkedItemList;
    }
}

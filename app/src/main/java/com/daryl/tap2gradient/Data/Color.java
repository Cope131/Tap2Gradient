package com.daryl.tap2gradient.Data;

public class Color {

    private String HEX;
    private int[] RGB;
    private float[] HSV;

    public Color() {
        // White is Default Color
        HEX = "#FFFFFF";
        RGB = new int[]{255, 255, 255};
        HSV = new float[]{0, 0 ,100};
    }

    // -> HEX
    public String getHEX() {
        return HEX;
    }

    public void setHEX(String HEX) {
        this.HEX = HEX;
    }

    // -> RGB
    public int[] getRGBInt() {
        return RGB;
    }

    public String getFormattedRGBString() {
        return String.format("%d, %d, %d", RGB[0], RGB[1], RGB[2]);
    }

    public String[] getRGBString() {
        return intToStringArray(RGB);
    }

    public void setRGBInt(int[] RGB) {
        this.RGB = RGB;
    }

    public void setRGBString(String[] RGB) {
        this.RGB = stringToIntArray(RGB);
    }

    // -> HSV
    public float[] getHSVInt() {
        return HSV;
    }

    public String getFormattedHSVString() {
        return String.format("%.1f, %.0f%%, %.0f%%", HSV[0], HSV[1], HSV[2]);
    }

    public String[] getHSVString() {
        return floatToStringArray(HSV);
    }

    public void setHSVInt(float[] HSV) {
        this.HSV = HSV;
    }

    public void setHSVString(String[] HSV) {
        this.HSV = stringToFloatArray(HSV);
    }

    // -> HEX, RGB, & HSV
    public String getAll() {
        return String.format("%s\n%s\n%s", getHEX(), getFormattedRGBString(), getFormattedHSVString());
    }


    private String[] intToStringArray(int[] intArray) {
        String[] stringArray = new String[3];
        for (int i = 0; i < intArray.length; i++) {
            stringArray[i] = intArray[i] + "";
        }
        return stringArray;
    }

    private int[] stringToIntArray(String[] stringArray) {
        int[] intArray = new int[3];
        for (int i = 0; i < stringArray.length; i++) {
            intArray[i] = Integer.parseInt(stringArray[i]);
        }
        return intArray;
    }

    private String[] floatToStringArray(float[] floatArray) {
        String[] stringArray = new String[3];
        for (int i = 0; i < floatArray.length; i++) {
            stringArray[i] = floatArray[i] + "";
        }
        return stringArray;
    }

    private float[] stringToFloatArray(String[] stringArray) {
        float[] floatArray = new float[3];
        for (int i = 0; i < stringArray.length; i++) {
            floatArray[i] = Float.parseFloat(stringArray[i]);
        }
        return floatArray;
    }



}

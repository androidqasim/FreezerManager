package de.geek_hub.freezermanager;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Date;

// only the name is mandatory, all other data is optional

public class Item implements Parcelable {
    private String name;
    private float size;
    private String unit;
    private Date freezeDate;
    private Date expDate;
    private String category;

    public Item(String name) {
        setName(name);
        setFreezeDate(new Date());
    }

    public float getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = size;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Date getExpDate() {
        return expDate;
    }

    public void setExpDate(Date expDate) {
        this.expDate = expDate;
    }

    public Date getFreezeDate() {
        return freezeDate;
    }

    public void setFreezeDate(Date freezeDate) {
        this.freezeDate = freezeDate;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    // Parcelable implementation

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.name);
        out.writeFloat(this.size);
        out.writeString(this.unit);
        out.writeLong(this.freezeDate.getTime());
        if (this.expDate == null) {
            out.writeLong(0);
        } else {
            out.writeLong(this.expDate.getTime());
        }
        out.writeString(this.category);
    }

    public static final Parcelable.Creator<Item> CREATOR = new Parcelable.Creator<Item>() {
        public Item createFromParcel(Parcel in) {
            return new Item(in);
        }

        public Item[] newArray(int size) {
            return new Item[size];
        }
    };

    private Item(Parcel in) {
        this.name = in.readString();
        this.size = in.readFloat();
        this.unit = in.readString();
        this.freezeDate = new Date(in.readLong());
        long expDate = in.readLong();
        if (expDate == 0) {
            this.expDate = null;
        } else {
            this.expDate = new Date(expDate);
        }
        this.category = in.readString();
    }
}
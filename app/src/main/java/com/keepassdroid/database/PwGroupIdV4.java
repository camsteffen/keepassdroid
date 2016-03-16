package com.keepassdroid.database;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.UUID;

public class PwGroupIdV4 extends PwGroupId {
	private UUID uuid;
	
	public PwGroupIdV4(UUID u) {
		uuid = u;
	}
	
	@Override
	public boolean equals(Object id) {
		if ( ! (id instanceof PwGroupIdV4) ) {
			return false;
		}
		
		PwGroupIdV4 v4 = (PwGroupIdV4) id;
		
		return uuid.equals(v4.uuid);
	}
	
	@Override
	public int hashCode() {
		return uuid.hashCode();
	}
	
	public UUID getId() {
		return uuid;
	}

	public static final Parcelable.Creator<PwGroupIdV4> CREATOR
			= new Parcelable.Creator<PwGroupIdV4>() {
		@Override
		public PwGroupIdV4 createFromParcel(Parcel source) {
			UUID uuid = (UUID) source.readSerializable();
			return new PwGroupIdV4(uuid);
		}

		@Override
		public PwGroupIdV4[] newArray(int size) {
			return new PwGroupIdV4[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeSerializable(uuid);
	}
}

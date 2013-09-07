package org.gscript.process;

import org.gscript.EmulatorActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

public class ProcessDescriptor implements Parcelable {

	int mPid;
	Intent mIntent;
	long mTime;

	public ProcessDescriptor(int pid, Intent i) {
		this.mPid = pid;
		this.mIntent = i;
		this.mTime = System.currentTimeMillis();
	}

	public ProcessDescriptor(int pid, Intent i, long time) {
		this.mPid = pid;
		this.mIntent = i;
		this.mTime = time;
	}

	public ProcessDescriptor(ProcessDescriptor pd) {
		this.mPid = pd.mPid;
		this.mIntent = pd.mIntent;
		this.mTime = pd.mTime;
	}

	/* the pid the process was assigned */
	public int getPid() {
		return this.mPid;
	}

	/* the intent the process was created from */
	public Intent getIntent() {
		return this.mIntent;
	}

	public long getTime() {
		return this.mTime;
	}

	@Override
	public boolean equals(Object o) {
		boolean result = false;
		if (o instanceof ProcessDescriptor) {

			ProcessDescriptor opd = (ProcessDescriptor) o;
			result = ((mPid == opd.mPid)
					&& (mTime == opd.mTime));
			//&& (mIntent.toUri(0).equals(opd.mIntent.toUri(0)))
		}
		return result;
	}

	@Override
	public int hashCode() {

		int result = 17;

		result = 31 * result + mPid;
		result = 31 * result + ((mIntent == null) ? 0 : mIntent.hashCode());
		result = 31 * result + (int) (mTime ^ (mTime >>> 32));

		return result;
	}

	@Override
	public String toString() {
		return getClass().getName() + "[" + "pid=" + mPid + ", " + "intent="
				+ mIntent + ", " + "time=" + mTime + "]";
	}

	public Intent getActivityIntent(Context c) {

		Intent activityIntent = new Intent(c, EmulatorActivity.class);

		activityIntent.putExtra(EmulatorActivity.EXTRA_DESCRIPTOR, this);
		activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TOP);

		return activityIntent;
	}

	public static final Parcelable.Creator<ProcessDescriptor> CREATOR = new Parcelable.Creator<ProcessDescriptor>() {

		public ProcessDescriptor createFromParcel(Parcel in) {
			return new ProcessDescriptor(in);
		}

		public ProcessDescriptor[] newArray(int size) {
			return new ProcessDescriptor[size];
		}

	};

	public ProcessDescriptor(Parcel source) {
		readFromParcel(source);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mPid);
		dest.writeLong(mTime);
		dest.writeParcelable(mIntent, flags);
	}

	public void readFromParcel(Parcel source) {
		mPid = source.readInt();
		mTime = source.readLong();
		mIntent = source.readParcelable(Intent.class.getClassLoader());
	}
}

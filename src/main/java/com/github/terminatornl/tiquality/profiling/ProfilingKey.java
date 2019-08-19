package com.github.terminatornl.tiquality.profiling;

public class ProfilingKey {

    private final long profileEndTime;

    public ProfilingKey(long profileEndTime) {
        this.profileEndTime = profileEndTime;
    }

    public long getProfileEndTime() {
        return profileEndTime;
    }
}

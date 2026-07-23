package com.signaltrace.portfolio;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;

public class PortfolioSyncJobService extends JobService {
    private static final int SYNC_JOB_ID = 4102;
    private static final long SYNC_INTERVAL_MS = 12L * 60L * 60L * 1000L;

    static void schedule(Context context) {
        JobScheduler scheduler = context.getSystemService(JobScheduler.class);
        if (scheduler == null) return;
        JobInfo job = new JobInfo.Builder(SYNC_JOB_ID, new ComponentName(context, PortfolioSyncJobService.class))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPersisted(true)
            .setPeriodic(SYNC_INTERVAL_MS)
            .build();
        scheduler.schedule(job);
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        new Thread(() -> {
            PortfolioSync.sync(getApplicationContext());
            jobFinished(params, false);
        }).start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}

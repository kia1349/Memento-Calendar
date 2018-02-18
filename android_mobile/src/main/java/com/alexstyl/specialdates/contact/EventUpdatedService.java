package com.alexstyl.specialdates.contact;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.alexstyl.specialdates.AppComponent;
import com.alexstyl.specialdates.EasyPreferences;
import com.alexstyl.specialdates.MementoApplication;
import com.alexstyl.specialdates.R;
import com.alexstyl.specialdates.events.ContactsObserver;
import com.alexstyl.specialdates.events.PeopleEventsMonitor;
import com.alexstyl.specialdates.events.PreferenceChangedEventsUpdateTrigger;
import com.alexstyl.specialdates.events.peopleevents.EventPreferences;

import javax.inject.Inject;
import java.util.concurrent.Callable;

import io.reactivex.Observable;

import static java.util.Arrays.asList;

public class EventUpdatedService extends Service {

    private static final int A_NUMBER = 5;
    
    @Inject PeopleEventsMonitor eventsMonitor;
    @Inject EventPreferences eventPreferences;

    private PreferenceChangedEventsUpdateTrigger preferenceChangedEventsUpdateTrigger;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        AppComponent applicationModule = ((MementoApplication) getApplication()).getApplicationModule();
        applicationModule.inject(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        setupDatabaseRefresher();
        return START_STICKY;
    }

    private void setupDatabaseRefresher() {
        preferenceChangedEventsUpdateTrigger = new PreferenceChangedEventsUpdateTrigger(
                EasyPreferences.createForDefaultPreferences(this),
                getResources(),
                R.string.key_enable_bank_holidays,
                R.string.key_enable_namedays,
                R.string.key_nameday_lang,
                R.string.key_namedays_contacts_only,
                R.string.key_namedays_full_name
        );

        eventsMonitor.startMonitoring(
                asList(
                        new ContactsObserver(getContentResolver()),
                        preferenceChangedEventsUpdateTrigger
                ));

        boolean eventsHaveBeenInitialised = eventPreferences.hasBeenInitialised();
        if (!eventsHaveBeenInitialised) {
            Observable.fromCallable(new Callable<Integer>() {
                @Override
                public Integer call() {
                    eventsMonitor.updateEvents();
                    eventPreferences.markEventsAsInitialised();
                    return A_NUMBER;
                }
            })
                    .subscribe();
        }
    }
}

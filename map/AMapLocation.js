/**
 * Created by lgzhuo on 2017/8/8.
 */
import {Platform, PermissionsAndroid, NativeModules, NativeEventEmitter} from 'react-native'

const AMapLocationObserver = NativeModules.AMapLocationObserver;
const invariant = require('fbjs/lib/invariant');
let updatesEnabled = false;
let subscriptions = [];

const LocationEventEmitter = new NativeEventEmitter(AMapLocationObserver);

type AMapLocationOptions = {
    timeout: number,
    maximumAge: number,
    enableHighAccuracy: bool,
    distanceFilter: number,
    interval: number,
    needAddress: bool
}

const logError = error => console.debug('AMapLocation error', error);

const LocationObserver = {

    /*
   * Invokes the success callback once with the latest location info.  Supported
   * options: timeout (ms), maximumAge (ms), enableHighAccuracy (bool)
   * On Android, if the location is cached this can return almost immediately,
   * or it will request an update which might take a while.
   */
    getCurrentPosition: async function (success: Function,
                                        error?: Function,
                                        options?: AMapLocationOptions) {
        invariant(
            typeof success === 'function',
            'Must provide a valid geo_success callback.'
        );
        let hasPermission = true;
        // Supports Android's new permission model. For Android older devices,
        // it's always on.
        if (Platform.OS === 'android' && Platform.Version >= 23) {
            hasPermission = await PermissionsAndroid.check(
                PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
            );
            if (!hasPermission) {
                const status = await PermissionsAndroid.request(
                    PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
                );
                hasPermission = status === PermissionsAndroid.RESULTS.GRANTED;
            }
        }
        if (hasPermission) {
            AMapLocationObserver.getCurrentPosition(
                options || {},
                success,
                error || logError,
            );
        }
    },

    /*
   * Invokes the success callback whenever the location changes.  Supported
   * options: timeout (ms), maximumAge (ms), enableHighAccuracy (bool), distanceFilter(m)
   */
    watchPosition: function (success: Function, error?: Function, options?: AMapLocationOptions): number {
        if (!updatesEnabled) {
            AMapLocationObserver.startObserving(options || {});
            updatesEnabled = true;
        }
        const watchID = subscriptions.length;
        subscriptions.push([
            LocationEventEmitter.addListener(
                'AMapLocationDidChange',
                success
            ),
            error ? LocationEventEmitter.addListener(
                'AMapLocationError',
                error
            ) : null,
        ]);
        return watchID;
    },

    clearWatch: function (watchID: number) {
        const sub = subscriptions[watchID];
        if (!sub) {
            // Silently exit when the watchID is invalid or already cleared
            // This is consistent with timers
            return;
        }

        sub[0].remove();
        // array element refinements not yet enabled in Flow
        const sub1 = sub[1];
        sub1 && sub1.remove();
        subscriptions[watchID] = undefined;
        let noWatchers = true;
        for (let ii = 0; ii < subscriptions.length; ii++) {
            if (subscriptions[ii]) {
                noWatchers = false; // still valid subscriptions
            }
        }
        if (noWatchers) {
            LocationObserver.stopObserving();
        }
    },

    stopObserving: function () {
        if (updatesEnabled) {
            AMapLocationObserver.stopObserving();
            updatesEnabled = false;
            for (let ii = 0; ii < subscriptions.length; ii++) {
                const sub = subscriptions[ii];
                if (sub) {
                    warning('Called stopObserving with existing subscriptions.');
                    sub[0].remove();
                    // array element refinements not yet enabled in Flow
                    const sub1 = sub[1];
                    sub1 && sub1.remove();
                }
            }
            subscriptions = [];
        }
    }
};

export default LocationObserver
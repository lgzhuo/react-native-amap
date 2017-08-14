//
//  AMapLocationObserver.m
//  Pods
//
//  Created by yunrui on 2017/8/8.
//
//

#import "AMapLocationObserver.h"
#import <AMapLocationKit/AMapLocationKit.h>

#import <React/RCTConvert.h>

#define AMAP_DEFAULT_LOCATION_ACCURACY kCLLocationAccuracyHundredMeters

typedef struct {
    double timeout;
    double maximumAge;
    double accuracy;
    double distanceFilter;
    BOOL needAddress;
} AMapLocationOptions;

@implementation RCTConvert (AMapLocationOptions)

+ (AMapLocationOptions)AMapLocationOptions:(id)json
{
    NSDictionary<NSString *, id> *options = [RCTConvert NSDictionary:json];
    
    double distanceFilter = options[@"distanceFilter"] == NULL ? AMAP_DEFAULT_LOCATION_ACCURACY
    : [RCTConvert double:options[@"distanceFilter"]] ?: kCLDistanceFilterNone;
    
    return (AMapLocationOptions){
        .timeout = [RCTConvert NSTimeInterval:options[@"timeout"]] ?: INFINITY,
        .maximumAge = [RCTConvert NSTimeInterval:options[@"maximumAge"]] ?: INFINITY,
        .accuracy = [RCTConvert BOOL:options[@"enableHighAccuracy"]] ? kCLLocationAccuracyBest : AMAP_DEFAULT_LOCATION_ACCURACY,
        .distanceFilter = distanceFilter,
        .needAddress = [RCTConvert BOOL:options[@"needAddress"]] ?: NO,
    };
}
@end

@implementation AMapLocationReGeocode (AMapLocationEvent)

- (NSDictionary<NSString *, id> *) event{
    
    return @{@"adCode":self.adcode?:[NSNull null],
             @"address":self.formattedAddress?:[NSNull null],
             @"aoiName":self.AOIName?:[NSNull null],
             @"city":self.city?:[NSNull null],
             @"cityCode":self.citycode?:[NSNull null],
             @"country":self.country?:[NSNull null],
             @"district":self.district?:[NSNull null],
             @"poiName":self.POIName?:[NSNull null],
             @"province":self.province?:[NSNull null],
             @"street":self.street?:[NSNull null],
             @"streetNum":self.number?:[NSNull null]};
}

@end

typedef NS_ENUM(NSInteger, RCTPositionErrorCode) {
    RCTPositionErrorDenied = 1,
    RCTPositionErrorUnavailable,
    RCTPositionErrorTimeout,
};

static NSDictionary<NSString *, id> *RCTPositionError(RCTPositionErrorCode code, NSString *msg /* nil for default */)
{
    if (!msg) {
        switch (code) {
            case RCTPositionErrorDenied:
                msg = @"User denied access to location services.";
                break;
            case RCTPositionErrorUnavailable:
                msg = @"Unable to retrieve location.";
                break;
            case RCTPositionErrorTimeout:
                msg = @"The location request timed out.";
                break;
        }
    }
    
    return @{
             @"code": @(code),
             @"message": msg,
             @"PERMISSION_DENIED": @(RCTPositionErrorDenied),
             @"POSITION_UNAVAILABLE": @(RCTPositionErrorUnavailable),
             @"TIMEOUT": @(RCTPositionErrorTimeout)
             };
}

static NSDictionary<NSString *, id> *RCTLocationEvent(CLLocation *location, AMapLocationReGeocode *regeocode){
    return @{
             @"coords": @{
                     @"latitude": @(location.coordinate.latitude),
                     @"longitude": @(location.coordinate.longitude),
                     @"altitude": @(location.altitude),
                     @"accuracy": @(location.horizontalAccuracy),
                     @"altitudeAccuracy": @(location.verticalAccuracy),
                     @"heading": @(location.course),
                     @"speed": @(location.speed),
                     @"bearing": @(location.course)
                     },
             @"timestamp": @([location.timestamp timeIntervalSince1970] * 1000), // in ms
             @"regeocode":[regeocode event]?:[NSNull null]
             };
}

@interface OnceLocationRequest : NSObject
@property (nonatomic, copy) RCTResponseSenderBlock successBlock;
@property (nonatomic, copy) RCTResponseSenderBlock errorBlock;
@property (nonatomic, assign) AMapLocationOptions options;
@property (nonatomic, strong) NSTimer *timeoutTimer;
@property (nonatomic, weak) id target;
@property (nonatomic, assign) SEL onComplete;

@property (nonatomic, readonly) CLLocationAccuracy accuracy;

- (void) invoke:(AMapLocationManager*) locationManager;
@end

@implementation OnceLocationRequest

- (void) invoke:(AMapLocationManager *)locationManager{
    locationManager.locationTimeout = self.options.timeout;
    locationManager.desiredAccuracy = MIN(locationManager.desiredAccuracy, self.options.accuracy);
    AMapLocatingCompletionBlock completionBlock = ^(CLLocation *location, AMapLocationReGeocode *regeocode, NSError *error) {
        if (_target && _onComplete && [_target respondsToSelector:_onComplete]) {
            IMP imp = [_target methodForSelector:_onComplete];
            void (*func)(id, SEL, OnceLocationRequest *, CLLocation *, AMapLocationReGeocode *, NSError *) = (void *)imp;
            func(_target, _onComplete, self, location, regeocode, error);
        }
    };
    if (![locationManager requestLocationWithReGeocode:self.options.needAddress completionBlock:completionBlock]) {
        completionBlock(nil,nil,[NSError errorWithDomain:NSCocoaErrorDomain code:AMapLocationErrorUnknown userInfo:nil]);
    }
}

- (CLLocationAccuracy)accuracy{
    return self.options.accuracy;
}

@end

@interface AMapLocationObserver () <AMapLocationManagerDelegate>
@property(nonatomic,strong)AMapLocationManager *locationManager;
@end
@implementation AMapLocationObserver{
    BOOL _observingLocation;
    NSDictionary<NSString *,id> *_lastLocationEvent;
    NSMutableArray<OnceLocationRequest *> *_pendingRequests;
    AMapLocationOptions _observerOptions;
}

#pragma mark - overide

- (NSArray<NSString *> *)supportedEvents{
    return @[@"AMapLocationDidChange", @"AMapLocationError"];
}

#pragma mark - RCT export
    
RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(startObserving:(AMapLocationOptions)options){
    _observerOptions = options;
    _observingLocation = YES;
    self.locationManager.distanceFilter = options.distanceFilter;
    self.locationManager.locatingWithReGeocode = options.needAddress;
    [self updateAccuracy];
    [self.locationManager startUpdatingLocation];
}

RCT_EXPORT_METHOD(stopObserving)
{
    // Stop observing
    _observingLocation = NO;
    [self stopUpdatingLocationSafely];
}

RCT_EXPORT_METHOD(getCurrentPosition:(AMapLocationOptions)options
                  withSuccessCallback:(RCTResponseSenderBlock)successBlock
                  errorCallback:(RCTResponseSenderBlock)errorBlock)
{
    if (!successBlock) {
        RCTLogError(@"%@.getCurrentPosition called with nil success parameter.", [self class]);
        return;
    }
    
    // Check if previous recorded location exists and is good enough
    if (_lastLocationEvent &&
        [NSDate date].timeIntervalSince1970 - [RCTConvert NSTimeInterval:_lastLocationEvent[@"timestamp"]] < options.maximumAge &&
        [_lastLocationEvent[@"coords"][@"accuracy"] doubleValue] <= options.accuracy && (!options.needAddress || [_lastLocationEvent[@"regeocode"] isKindOfClass:[NSDictionary class]])) {
        
        // Call success block with most recent known location
        successBlock(@[_lastLocationEvent]);
        return;
    }
    
    // Create request
    OnceLocationRequest *request = [OnceLocationRequest new];
    request.successBlock = successBlock;
    request.errorBlock = errorBlock ?: ^(NSArray *args){};
    request.options = options;
    request.target = self;
    request.onComplete = @selector(onCompleteWithRequest:location:regeocode:error:);
    
    if (!_pendingRequests) {
        _pendingRequests = [NSMutableArray new];
    }
    [_pendingRequests addObject:request];
    [request invoke:self.locationManager];
}

#pragma mark - private method

- (void) updateAccuracy{
    __block CLLocationAccuracy accuracy = _observingLocation?_observerOptions.accuracy:AMAP_DEFAULT_LOCATION_ACCURACY;
    [_pendingRequests enumerateObjectsUsingBlock:^(OnceLocationRequest * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        accuracy = MIN(accuracy, obj.options.accuracy);
    }];
    self.locationManager.desiredAccuracy = accuracy;
}

- (void) stopUpdatingLocationSafely{
    if (![_pendingRequests count] && !_observingLocation) {
        [self.locationManager stopUpdatingLocation];
    }
}

- (void) onCompleteWithRequest:(OnceLocationRequest *)request location:(CLLocation *)location regeocode:(AMapLocationReGeocode *)regeocode error:(NSError *)error{
    [_pendingRequests removeObject:request];
    [self updateAccuracy];
    if (error) {
        NSDictionary<NSString *, id> *jsError = nil;
        switch (error.code) {
            case AMapLocationErrorTimeOut:
                jsError = RCTPositionError(RCTPositionErrorTimeout, nil);
                break;
            case AMapLocationErrorNotConnectedToInternet:
            case AMapLocationErrorBadURL:
            case AMapLocationErrorCannotFindHost:
                jsError = RCTPositionError(RCTPositionErrorUnavailable, @"Unable to retrieve location due to a network failure");
                break;
            case AMapLocationErrorLocateFailed:
            default:
                jsError = RCTPositionError(RCTPositionErrorUnavailable, nil);
                break;
        }
        request.errorBlock(@[jsError]);
    }else{
        request.successBlock(@[RCTLocationEvent(location, regeocode)]);
    }
    [self stopUpdatingLocationSafely];
}

#pragma mark - getter & setter

-(AMapLocationManager*)locationManager{
    if (!_locationManager) {
        _locationManager = [[AMapLocationManager alloc]init];
        _locationManager.delegate = self;
        
        NSArray *backgroundModes  = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"UIBackgroundModes"];
        if (backgroundModes && [backgroundModes containsObject:@"location"]) {
            [_locationManager setPausesLocationUpdatesAutomatically:NO];
            if ([_locationManager respondsToSelector:@selector(setAllowsBackgroundLocationUpdates:)]) {
                [_locationManager setAllowsBackgroundLocationUpdates:YES];
            }
        }
    }
    return _locationManager;
}

#pragma mark - AMapLocationManagerDelegate

- (void)amapLocationManager:(AMapLocationManager *)manager didUpdateLocation:(CLLocation *)location reGeocode:(AMapLocationReGeocode *)reGeocode{
    _lastLocationEvent = RCTLocationEvent(location, reGeocode);
    
    // Send event
    if (_observingLocation) {
        [self sendEventWithName:@"AMapLocationDidChange" body:_lastLocationEvent];
    }
}

- (void)amapLocationManager:(AMapLocationManager *)manager didFailWithError:(NSError *)error{
    NSDictionary<NSString *, id> *jsError = nil;
    switch (error.code) {
        case kCLErrorDenied:
            jsError = RCTPositionError(RCTPositionErrorDenied, nil);
            break;
        case kCLErrorNetwork:
            jsError = RCTPositionError(RCTPositionErrorUnavailable, @"Unable to retrieve location due to a network failure");
            break;
        case kCLErrorLocationUnknown:
        default:
            jsError = RCTPositionError(RCTPositionErrorUnavailable, nil);
            break;
    }
    
    if (_observingLocation) {
        [self sendEventWithName:@"AMapLocationError" body:jsError];
    }
}
    
@end

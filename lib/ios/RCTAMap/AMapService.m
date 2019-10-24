//
//  AMapService.m
//  Awesome
//
//  Created by yunrui on 2017/3/17.
//  Copyright © 2017年 Facebook. All rights reserved.
//

#import "AMapService.h"
#import <MAMapKit/MAMapKit.h>
#import <AMapNaviKit/AMapNaviKit.h>
#import "RCTConvert+MAMap.h"
#import "AMapNaviRoute+ID.h"
#import "Convert2Json.h"
#import <AMapFoundationKit/AMapFoundationKit.h>
#import <AMapLocationKit/AMapLocationKit.h>
#import <AMapSearchKit/AMapSearchKit.h>
#import <objc/runtime.h>
#import "DriveNaviViewController.h"
#import "SpeechSynthesizer.h"
#import "WalkNaviViewController.h"

#define AMapRequiredParameters @[@"sourceApplication",@"dlat",@"dlon",@"dev",@"t"]
#define AMapOptionalParameters @[@"sid",@"slat",@"slon",@"sname",@"did",@"dname"]
#define baiduMapRequiredParameters @[@"mode",@"src"]
#define baiduMapOptionalParameters @[@"origin",@"destination",@"region",@"origin_region",@"destination_region",@"coord_type",@"zoom"]

@interface SingleLocationDelegate : NSObject<AMapLocationManagerDelegate>
@property(nonatomic,copy)RCTPromiseRejectBlock reject;
@property(nonatomic,copy)RCTPromiseResolveBlock resolve;
-(instancetype)initWithResolve:(RCTPromiseResolveBlock) resolve reject:(RCTPromiseRejectBlock) reject;
@end

@implementation SingleLocationDelegate

-(instancetype)initWithResolve:(RCTPromiseResolveBlock) resolve reject:(RCTPromiseRejectBlock) reject{
    if (self = [super init]) {
        _resolve = resolve;
        _reject = reject;
    }
    return self;
}
@end

@interface AMapPOISearchBaseRequest(RCTAMap)
@property(nonatomic,copy)RCTPromiseRejectBlock reject;
@property(nonatomic,copy)RCTPromiseResolveBlock resolve;
@end

@implementation AMapPOISearchBaseRequest(RCTAMap)
-(void)setReject:(RCTPromiseRejectBlock)_reject{
    objc_setAssociatedObject(self, @selector(reject), _reject, OBJC_ASSOCIATION_COPY_NONATOMIC);
}
-(RCTPromiseRejectBlock)reject{
    return objc_getAssociatedObject(self, _cmd);
}

-(void)setResolve:(RCTPromiseResolveBlock)_resolve{
    objc_setAssociatedObject(self, @selector(resolve), _resolve, OBJC_ASSOCIATION_COPY_NONATOMIC);
}
-(RCTPromiseResolveBlock)resolve{
    return objc_getAssociatedObject(self, _cmd);
}

@end

@interface AMapService()<AMapNaviDriveManagerDelegate,AMapSearchDelegate,DriveNaviViewControllerDelegate,AMapNaviWalkManagerDelegate,WalkNaviViewControllerDelegate>
@property(nonatomic,strong) AMapNaviDriveManager *naviDriveManager;
@property(nonatomic,strong) AMapNaviWalkManager *naviWalkManager;
@property(nonatomic,strong) AMapLocationManager *locationManager;
@property(nonatomic,strong) AMapSearchAPI *searchApi;
@property(nonatomic,readonly) UIViewController* rootVC;
@end

@implementation AMapService{
  RCTPromiseResolveBlock _naviDriveResolve;
  RCTPromiseRejectBlock _naviDriveReject;
    
  RCTPromiseResolveBlock _naviWalkResolve;
  RCTPromiseRejectBlock _naviWalkReject;
}

RCT_EXPORT_MODULE(AMapService)

RCT_EXPORT_METHOD(calculateNaviDriveRoute:(NSDictionary*)props resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject){
  if(!props || ![[props allKeys]containsObject:@"to"]){
    reject(@"-2", @"参数不完整", nil);
    return;
  }
  BOOL avoidCongestion = [RCTConvert BOOL:props[@"avoidCongestion"]],
       avoidHighway = [RCTConvert BOOL:props[@"avoidHighway"]],
       avoidCost = [RCTConvert BOOL:props[@"avoidCost"]],
       prioritiseHighway = [RCTConvert BOOL:props[@"prioritiseHighway"]],
       multipleRoute = [RCTConvert BOOL:props[@"multipleRoute"]];
  if (avoidHighway && prioritiseHighway) {
    reject(@"-3", @"不走高速与高速优先不能同时为true.", nil);
    return;
  }
  if (avoidCost && prioritiseHighway) {
    reject(@"-4", @"高速优先与避免收费不能同时为true.", nil);
    return;
  }
  if(_naviDriveReject){
    _naviDriveReject(@"-1", @"有新的导航规划请求，当前路径规划已取消", nil);
    _naviDriveReject = nil;
    _naviDriveResolve = nil;
  }
  
  AMapNaviPoint *to = [RCTConvert AMapNaviPoint:props[@"to"]];
  AMapNaviPoint *from;
  if([[props allKeys]containsObject:@"from"]){
    from = [RCTConvert AMapNaviPoint:props[@"from"]];
  }
  NSArray<AMapNaviPoint*> *wayPoints;
  if([[props allKeys]containsObject:@"wayPoints"]){
    wayPoints = [RCTConvert AMapNaviPointArray:props[@"wayPoints"]];
  }
  AMapNaviDrivingStrategy strategy = ConvertDrivingPreferenceToDrivingStrategy(multipleRoute,
                                                                               avoidCongestion,
                                                                               avoidHighway,
                                                                               avoidCost,
                                                                               prioritiseHighway);
  BOOL r;
  if(from){
    r = [self.naviDriveManager calculateDriveRouteWithStartPoints:@[from]
                                                        endPoints:@[to]
                                                        wayPoints:wayPoints
                                                  drivingStrategy:strategy];
  }else{
    r = [self.naviDriveManager calculateDriveRouteWithEndPoints:@[to]
                                                      wayPoints:wayPoints
                                                drivingStrategy:strategy];
  }
  if(r){
    _naviDriveResolve = resolve;
    _naviDriveReject = reject;
  }else{
    reject(@"-5", @"导航线路规划失败", nil);
  }
}

RCT_EXPORT_METHOD(calculateNaviWalkRoute:(NSDictionary*)props resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject){
    if(!props || ![[props allKeys]containsObject:@"to"]){
        reject(@"-2", @"参数不完整", nil);
        return;
    }
    if(_naviWalkReject){
        _naviWalkReject(@"-1", @"有新的导航规划请求，当前路径规划已取消", nil);
        _naviWalkReject = nil;
        _naviWalkResolve = nil;
    }
    
    AMapNaviPoint *to = [RCTConvert AMapNaviPoint:props[@"to"]];
    AMapNaviPoint *from;
    if([[props allKeys]containsObject:@"from"]){
        from = [RCTConvert AMapNaviPoint:props[@"from"]];
    }
    
    BOOL r;
    if(from){
        r = [self.naviWalkManager calculateWalkRouteWithStartPoints:@[from] endPoints:@[to]];
    }else{
        r = [self.naviWalkManager calculateWalkRouteWithEndPoints:@[to]];
    }
    if(r){
        _naviWalkResolve = resolve;
        _naviWalkReject = reject;
    }else{
        reject(@"-5", @"导航线路规划失败", nil);
    }
}

RCT_EXPORT_METHOD(startNavi:(NSDictionary*)props){
    if ([@"walk" isEqualToString:props[@"type"]]) {
        WalkNaviViewController *walkVC = [[WalkNaviViewController alloc]init];
        walkVC.delegate = self;
        [self.naviWalkManager addDataRepresentative:walkVC.walkView];
        dispatch_async(dispatch_get_main_queue(), ^{
            [self.rootVC presentViewController:walkVC animated:NO completion:nil];
            [self.naviWalkManager startGPSNavi];
        });
    }else{
        DriveNaviViewController *driveVC = [[DriveNaviViewController alloc] init];
        [driveVC setDelegate:self];
        
        if ([props.allKeys containsObject:@"id"]) {
            [self.naviDriveManager selectNaviRouteWithRouteID:[RCTConvert NSInteger:props[@"id"]]];
        }
        
        //将driveView添加为导航数据的Representative，使其可以接收到导航诱导数据
        [self.naviDriveManager addDataRepresentative:driveVC.driveView];
        
        dispatch_async(dispatch_get_main_queue(), ^{
            [self.rootVC presentViewController:driveVC animated:NO completion:nil];
            [self.naviDriveManager startGPSNavi];
        });
    }
}

RCT_EXPORT_METHOD(getCurrentPosition:(NSDictionary*)props resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject){
    self.locationManager.desiredAccuracy = kCLLocationAccuracyBest;
    [self.locationManager requestLocationWithReGeocode:YES completionBlock:^(CLLocation *location, AMapLocationReGeocode *regeocode, NSError *error) {
        if (error) {
            reject([NSString stringWithFormat:@"%ld",(long)error.code],error.description,error);
        }else{
            resolve(@{@"latitude":@(location.coordinate.latitude),
                      @"longitude":@(location.coordinate.longitude),
                      @"accuracy":@(location.horizontalAccuracy),
                      @"adCode":regeocode.adcode?regeocode.adcode:[NSNull null],
                      @"address":regeocode.formattedAddress?regeocode.formattedAddress:[NSNull null],
                      @"altitude":@(location.altitude),
                      @"aoiName":regeocode.AOIName?regeocode.AOIName:[NSNull null],
                      @"bearing":@(location.course),
                      @"city":regeocode.city?regeocode.city:[NSNull null],
                      @"cityCode":regeocode.citycode?regeocode.citycode:[NSNull null],
                      @"country":regeocode.country?regeocode.country:[NSNull null],
                      @"district":regeocode.district?regeocode.district:[NSNull null],
                      @"floor":@(location.floor.level),
                      @"poiName":regeocode.POIName?regeocode.POIName:[NSNull null],
                      @"province":regeocode.province?regeocode.province:[NSNull null],
                      @"speed":@(location.speed),
                      @"street":regeocode.street?regeocode.street:[NSNull null],
                      @"streetNum":regeocode.number?regeocode.number:[NSNull null]});
        }
    }];
}

RCT_EXPORT_METHOD(poiSearch:(NSDictionary*)props resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject){
    AMapPOIKeywordsSearchRequest *request = [[AMapPOIKeywordsSearchRequest alloc]init];
    request.keywords = [RCTConvert NSString:props[@"keyWord"]];
    request.city = [RCTConvert NSString:props[@"city"]];
    request.cityLimit = [RCTConvert BOOL:props[@"cityLimit"]];
    request.offset = [RCTConvert NSInteger:props[@"pageSize"]];
    request.page = [RCTConvert NSInteger:props[@"pageNum"]];
    request.reject = reject;
    request.resolve = resolve;
    [self.searchApi AMapPOIKeywordsSearch:request];
}

RCT_EXPORT_METHOD(poiAroundSearch:(NSDictionary*)props resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    AMapPOIAroundSearchRequest *request = [[AMapPOIAroundSearchRequest alloc] init];
    request.keywords = [RCTConvert NSString:props[@"keyWord"]];
    request.types = [RCTConvert NSString:props[@"types"]];
    request.location = [RCTConvert AMapGeoPoint:props[@"location"]];
    request.radius = [RCTConvert NSInteger:props[@"radius"]];
    request.city = [RCTConvert NSString:props[@"city"]];
    request.offset = [RCTConvert NSInteger:props[@"pageSize"]];
    request.page = [RCTConvert NSInteger:props[@"pageNum"]];
    request.reject = reject;
    request.resolve = resolve;
    [self.searchApi AMapPOIAroundSearch: request];
}

RCT_EXPORT_METHOD(callAMapRoute:(NSDictionary*)props){
    NSURLComponents *urlBuilder;
    if ([[UIApplication sharedApplication]canOpenURL:[NSURL URLWithString:@"iosamap://"]]) {
        urlBuilder = [NSURLComponents componentsWithString:@"iosamap://path"];
        NSMutableArray *queryItems = [NSMutableArray arrayWithCapacity:props.count];
        for (NSString * parameter in AMapRequiredParameters) {
            if (![[props allKeys]containsObject:parameter]) {
                NSLog(@"调起高德地图route必须参数%@未设置",parameter);
                return;
            }
            [queryItems addObject:[NSURLQueryItem queryItemWithName:parameter value:[props[parameter] description]]];
        }
        for (NSString * parameter in AMapOptionalParameters) {
            if (![[props allKeys]containsObject:parameter]) {
                continue;
            }
            [queryItems addObject:[NSURLQueryItem queryItemWithName:parameter value:[props[parameter] description]]];
        }
        urlBuilder.queryItems = queryItems;
    }else{
        urlBuilder = [NSURLComponents componentsWithString:@"http://uri.amap.com/navigation"];
        NSMutableArray *queryItems = [NSMutableArray arrayWithCapacity:props.count];
        for (NSString * parameter in AMapRequiredParameters) {
            if (![[props allKeys]containsObject:parameter]) {
                NSLog(@"调起高德地图route必须参数%@未设置",parameter);
                return;
            }
        }
        if ([props.allKeys containsObject:@"slat"] && [props.allKeys containsObject:@"slon"]) {
            NSString *from = [NSString stringWithFormat:@"%@,%@",props[@"slon"],props[@"slat"]];
            if ([props.allKeys containsObject:@"sname"]) {
                from = [NSString stringWithFormat:@"%@,%@",from,props[@"sname"]];
            }
            [queryItems addObject:[NSURLQueryItem queryItemWithName:@"from" value:from]];
        }
        NSString *to = [NSString stringWithFormat:@"%@,%@",props[@"dlon"],props[@"dlat"]];
        if ([props.allKeys containsObject:@"dname"]) {
            to = [NSString stringWithFormat:@"%@,%@",to,props[@"dname"]];
        }
        [queryItems addObject:[NSURLQueryItem queryItemWithName:@"to" value:to]];
        NSInteger dev = [RCTConvert NSInteger:props[@"dev"]];
        [queryItems addObject:[NSURLQueryItem queryItemWithName:@"coordinate" value:dev==0?@"gaode":@"wgs84"]];
        NSString *mode;
        switch ([RCTConvert NSInteger:props[@"t"]]) {
            case 0:
                mode = @"car";
                break;
            case 1:
                mode = @"bus";
                break;
            case 2:
                mode = @"walk";
                break;
            case 3:
                mode = @"ride";
                break;
            default:
                break;
        }
        if (mode) {
            [queryItems addObject:[NSURLQueryItem queryItemWithName:@"mode" value:mode]];
        }
        [queryItems addObject:[NSURLQueryItem queryItemWithName:@"src" value:[RCTConvert NSString:props[@"sourceApplication"]]]];
        urlBuilder.queryItems = queryItems;
    }
    NSLog(@"调起高德地图route->%@",urlBuilder.URL);
    if ([[UIDevice currentDevice].systemVersion intValue]>10) {
        [[UIApplication sharedApplication] openURL:urlBuilder.URL options:@{} completionHandler:^(BOOL success) {
            
        }];
    }else{
        [[UIApplication sharedApplication] openURL:urlBuilder.URL];
    }
}

RCT_EXPORT_METHOD(callBaiduMapRoute:(NSDictionary*)props){
    NSURLComponents *urlBuilder;
    NSMutableArray *queryItems = [NSMutableArray arrayWithCapacity:props.count];
    if ([[UIApplication sharedApplication]canOpenURL:[NSURL URLWithString:@"baidumap://"]]) {
        urlBuilder = [NSURLComponents componentsWithString:@"baidumap://map/direction"];
    }else{
        urlBuilder = [NSURLComponents componentsWithString:@"http://api.map.baidu.com/direction"];
        [queryItems addObject:[NSURLQueryItem queryItemWithName:@"output" value:@"html"]];
    }
    if (![props.allKeys containsObject:@"destination"]&&![props.allKeys containsObject:@"origin"]) {
        NSLog(@"调起百度地图route必须设置起点或终点");
        return;
    }
    for (NSString *parameter in baiduMapRequiredParameters) {
        if ([props.allKeys containsObject:parameter]) {
            [queryItems addObject:[NSURLQueryItem queryItemWithName:parameter value:[props[parameter] description]]];
        }else{
            NSLog(@"调起百度地图route必须设置%@参数",parameter);
            return;
        }
    }
    for (NSString *parameter in baiduMapOptionalParameters) {
        if ([props.allKeys containsObject:parameter]) {
            [queryItems addObject:[NSURLQueryItem queryItemWithName:parameter value:[props[parameter] description]]];
        }
    }
    urlBuilder.queryItems = queryItems;
    NSLog(@"调起百度地图route->%@",urlBuilder.URL);
    if ([[UIDevice currentDevice].systemVersion intValue]>10) {
        [[UIApplication sharedApplication] openURL:urlBuilder.URL options:@{} completionHandler:^(BOOL success) {
            
        }];
    }else{
        [[UIApplication sharedApplication] openURL:urlBuilder.URL];
    }
}

#pragma mark AMapNaviDriveManagerDelegate
/**
 *  发生错误时,会调用代理的此方法
 *
 *  @param error 错误信息
 */
- (void)driveManager:(AMapNaviDriveManager *)driveManager error:(NSError *)error{
    NSLog(@"drive manager error %@", error);
    if (_naviDriveReject) {
        _naviDriveReject([NSString stringWithFormat:@"%ld",error.code], error.domain ,error);
        _naviDriveReject = nil;
        _naviDriveResolve = nil;
    }
}

/**
 *  驾车路径规划成功后的回调函数
 */
- (void)driveManagerOnCalculateRouteSuccess:(AMapNaviDriveManager *)driveManager{
    if (_naviDriveReject && _naviDriveResolve) {
        if (driveManager.naviRoutes.count >0) {
            NSMutableArray *routeArr = [NSMutableArray arrayWithCapacity:driveManager.naviRoutes.count];
            for (NSNumber *aRouteID in [driveManager.naviRoutes allKeys]){
                AMapNaviRoute *route = [driveManager.naviRoutes objectForKey:aRouteID];
                route.routeID = [aRouteID integerValue];
                
                [routeArr addObject:[Convert2Json AMapNaviRoute:route]];
            }
            _naviDriveResolve(routeArr);
        } else if (driveManager.naviRoute){
            AMapNaviRoute *route = driveManager.naviRoute;
            route.routeID = driveManager.naviRouteID;
            _naviDriveResolve(@[[Convert2Json AMapNaviRoute:route]]);
        } else{
            _naviDriveReject(@"-6", @"没有可用路线", nil);
        }
        _naviDriveReject = nil;
        _naviDriveResolve = nil;
    }
}

/**
 *  驾车路径规划失败后的回调函数
 *
 *  @param error 错误信息,error.code参照AMapNaviCalcRouteState
 */
- (void)driveManager:(AMapNaviDriveManager *)driveManager onCalculateRouteFailure:(NSError *)error{
    NSLog(@"drive onCalculateRouteFailure %@", error);
    if (_naviDriveReject) {
        _naviDriveReject([NSString stringWithFormat:@"%ld",error.code], error.domain ,error);
        _naviDriveReject = nil;
        _naviDriveResolve = nil;
    }
}

- (void)driveManager:(AMapNaviDriveManager *)driveManager playNaviSoundString:(NSString *)soundString soundStringType:(AMapNaviSoundType)soundStringType{
    NSLog(@"playNaviSoundString:{%ld:%@}", (long)soundStringType, soundString);
    
    [[SpeechSynthesizer sharedSpeechSynthesizer] speakString:soundString];
}

#pragma mark AMapNaviWalkManagerDelegate

/**
 *  发生错误时,会调用代理的此方法
 *
 *  @param error 错误信息
 */
- (void)walkManager:(AMapNaviWalkManager *)walkManager error:(NSError *)error{
    NSLog(@"walk manager error %@", error);
    if (_naviWalkReject) {
        _naviWalkReject([NSString stringWithFormat:@"%ld",error.code], error.domain ,error);
        _naviWalkReject = nil;
        _naviWalkResolve = nil;
    }
}

/**
 *  步行路径规划成功后的回调函数
 */
- (void)walkManagerOnCalculateRouteSuccess:(AMapNaviWalkManager *)walkManager{
    if (_naviWalkReject && _naviWalkResolve) {
        if (walkManager.naviRoute){
            AMapNaviRoute *route = walkManager.naviRoute;
            route.routeID = walkManager.naviRouteID;
            _naviWalkResolve(@[[Convert2Json AMapNaviRoute:route]]);
        } else{
            _naviWalkReject(@"-6", @"没有可用路线", nil);
        }
        _naviWalkReject = nil;
        _naviWalkResolve = nil;
    }
}

/**
 *  步行路径规划失败后的回调函数
 *
 *  @param error 错误信息,error.code参照AMapNaviCalcRouteState
 */
- (void)walkManager:(AMapNaviWalkManager *)walkManager onCalculateRouteFailure:(NSError *)error{
    NSLog(@"walk onCalculateRouteFailure %@", error);
    if (_naviWalkReject) {
        _naviWalkReject([NSString stringWithFormat:@"%ld",error.code], error.domain ,error);
        _naviWalkReject = nil;
        _naviWalkResolve = nil;
    }
}

/**
 *  导航播报信息回调函数
 *
 *  @param soundString 播报文字
 *  @param soundStringType 播报类型,参考AMapNaviSoundType
 */
- (void)walkManager:(AMapNaviWalkManager *)walkManager playNaviSoundString:(NSString *)soundString soundStringType:(AMapNaviSoundType)soundStringType{
    NSLog(@"playNaviSoundString:{%ld:%@}", (long)soundStringType, soundString);
    
    [[SpeechSynthesizer sharedSpeechSynthesizer] speakString:soundString];
}

#pragma mark AMapSearchDelegate
/**
 * @brief 当请求发生错误时，会调用代理的此方法.
 * @param request 发生错误的请求.
 * @param error   返回的错误.
 */
- (void)AMapSearchRequest:(AMapPOIKeywordsSearchRequest*)request didFailWithError:(NSError *)error{
    if (request.reject) {
        request.reject([NSString stringWithFormat:@"%ld",error.code], error.domain ,error);
    }
}

/**
 * @brief POI查询回调函数
 * @param request  发起的请求，具体字段参考 AMapPOISearchBaseRequest 及其子类。
 * @param response 响应结果，具体字段参考 AMapPOISearchResponse 。
 */
- (void)onPOISearchDone:(AMapPOISearchBaseRequest *)request response:(AMapPOISearchResponse *)response{
    if (request.resolve) {
        request.resolve([Convert2Json AMapPOISearchResponse:response]);
    }
}

#pragma mark DriveNaviViewControllerDelegate
- (void)driveNaviViewCloseButtonClicked{
    [self.naviDriveManager stopNavi];
    [[SpeechSynthesizer sharedSpeechSynthesizer] stopSpeak];
    [self.rootVC dismissViewControllerAnimated:NO completion:nil];
}

#pragma mark WalkNaviViewControllerDelegate
-(void)walkNaviViewCloseButtonClicked{
    [self.naviWalkManager stopNavi];
    [[SpeechSynthesizer sharedSpeechSynthesizer] stopSpeak];
    [self.rootVC dismissViewControllerAnimated:NO completion:nil];
}

#pragma mark getter & setter
-(AMapNaviDriveManager *)naviDriveManager{
  if (!_naviDriveManager) {
    _naviDriveManager = [AMapNaviDriveManager sharedInstance];
    _naviDriveManager.delegate = self;
  }
  return _naviDriveManager;
}

-(AMapLocationManager*)locationManager{
    if (!_locationManager) {
        _locationManager = [[AMapLocationManager alloc]init];
    }
    return _locationManager;
}

-(AMapSearchAPI*)searchApi{
    if (!_searchApi) {
        _searchApi = [[AMapSearchAPI alloc]init];
        _searchApi.delegate = self;
    }
    return _searchApi;
}

- (UIViewController*) rootVC {
    UIViewController *root = [[[[UIApplication sharedApplication] delegate] window] rootViewController];
    while (root.presentedViewController != nil) {
        root = root.presentedViewController;
    }
    
    return root;
}

-(AMapNaviWalkManager *)naviWalkManager{
    if (!_naviWalkManager) {
        _naviWalkManager = [[AMapNaviWalkManager alloc]init];
        _naviWalkManager.delegate = self;
    }
    return _naviWalkManager;
}

@end

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

@interface AMapService()<AMapNaviDriveManagerDelegate>
@property(nonatomic,strong) AMapNaviDriveManager *naviDriveManager;
@end

@implementation AMapService{
  RCTPromiseResolveBlock _naviDriveResolve;
  RCTPromiseRejectBlock _naviDriveReject;
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

#pragma mark AMapNaviDriveManagerDelegate
/**
 *  发生错误时,会调用代理的此方法
 *
 *  @param error 错误信息
 */
- (void)driveManager:(AMapNaviDriveManager *)driveManager error:(NSError *)error{
  
}

/**
 *  驾车路径规划成功后的回调函数
 */
- (void)driveManagerOnCalculateRouteSuccess:(AMapNaviDriveManager *)driveManager{
  if (driveManager.naviRoutes.count <=0) {
    _naviDriveReject(@"-6", @"没有可用路线", nil);
  }else{
    NSMutableArray *routeArr = [NSMutableArray arrayWithCapacity:driveManager.naviRoutes.count];
    for (NSNumber *aRouteID in [driveManager.naviRoutes allKeys]){
      AMapNaviRoute *route = [driveManager.naviRoutes objectForKey:aRouteID];
      route.routeID = [aRouteID integerValue];
      
      [routeArr addObject:[Convert2Json AMapNaviRoute:route]];
    }
    _naviDriveResolve(routeArr);
  }
  _naviDriveReject = nil;
  _naviDriveResolve = nil;
}

/**
 *  驾车路径规划失败后的回调函数
 *
 *  @param error 错误信息,error.code参照AMapNaviCalcRouteState
 */
- (void)driveManager:(AMapNaviDriveManager *)driveManager onCalculateRouteFailure:(NSError *)error{
  _naviDriveReject([NSString stringWithFormat:@"%ld",error.code], error.domain ,error);
  _naviDriveReject = nil;
  _naviDriveResolve = nil;
}

#pragma mark getter & setter
-(AMapNaviDriveManager *)naviDriveManager{
  if (!_naviDriveManager) {
    _naviDriveManager = [[AMapNaviDriveManager alloc]init];
    _naviDriveManager.delegate = self;
  }
  return _naviDriveManager;
}

@end

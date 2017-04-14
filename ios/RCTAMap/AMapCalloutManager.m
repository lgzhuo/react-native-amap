//
//  AMapCalloutManager.m
//  Awesome
//
//  Created by yunrui on 2017/3/14.
//  Copyright © 2017年 Facebook. All rights reserved.
//

#import "AMapCalloutManager.h"
#import "AMapCallout.h"

@implementation AMapCalloutManager

RCT_EXPORT_MODULE(AMapCallout)

-(UIView *)view{
  return [[AMapCallout alloc]init];
}
@end

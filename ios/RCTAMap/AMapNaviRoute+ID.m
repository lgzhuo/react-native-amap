//
//  AMapNaviRoute+ID.m
//  Awesome
//
//  Created by yunrui on 2017/3/20.
//  Copyright © 2017年 Facebook. All rights reserved.
//

#import "AMapNaviRoute+ID.h"
#import <objc/runtime.h>

@implementation AMapNaviRoute (ID)

- (NSInteger)routeID
{
  return [objc_getAssociatedObject(self, _cmd) integerValue];
}

- (void)setRouteID:(NSInteger)routeID
{
  objc_setAssociatedObject(self, @selector(routeID), @(routeID), OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}
@end

//
//  AMapOverlay.h
//  RCTAMap
//
//  Created by lgzhuo on 2017/11/15.
//  Copyright © 2017年 lgzhuo. All rights reserved.
//

#import <MAMapKit/MAMapKit.h>

@protocol AMapOverlay <MAOverlay>
@required

@property (nonatomic, assign) NSInteger zIndex;

@end

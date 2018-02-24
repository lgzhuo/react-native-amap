//
//  ClusterPoint.h
//  Pods
//
//  Created by yunrui on 2017/4/11.
//
//

#import <Foundation/Foundation.h>

@interface ClusterPoint : NSObject
@property (nonatomic, assign) double latitude;
@property (nonatomic, assign) double longitude;
@property (nonatomic, copy) NSString *id;
@property (nonatomic, assign) NSUInteger idx;

+(instancetype)pointWithLatitude:(double)latitude longitude:(double)longitude id:(NSString*)id;
@end

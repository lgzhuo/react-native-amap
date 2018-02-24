//
//  AMapPolyline.m
//  Awesome
//
//  Created by yunrui on 2017/3/16.
//  Copyright © 2017年 Facebook. All rights reserved.
//

#import "AMapPolyline.h"

@implementation AMapPolyline
@synthesize zIndex = _zIndex;

- (void)setFillColor:(UIColor *)fillColor {
    _fillColor = fillColor;
    [self update];
}

- (void)setStrokeColor:(UIColor *)strokeColor {
    _strokeColor = strokeColor;
    [self update];
}

- (void)setStrokeWidth:(CGFloat)strokeWidth {
    _strokeWidth = strokeWidth;
    [self update];
}

- (void)setLineJoin:(MALineJoinType)lineJoin {
    _lineJoin = lineJoin;
    [self update];
}

- (void)setLineCap:(MALineCapType)lineCap {
    _lineCap = lineCap;
    [self update];
}

- (void)setMiterLimit:(CGFloat)miterLimit {
    _miterLimit = miterLimit;
    [self update];
}

-(void)setLineDash:(BOOL)lineDash{
    _lineDash = lineDash;
    [self update];
}

-(void)setZIndex:(NSInteger)zIndex{
    _zIndex = zIndex;
    [self update];
}

- (void)setCoordinates:(NSArray<NSValue *> *)coordinates {
    _coordinates = coordinates;
    CLLocationCoordinate2D coords[coordinates.count];
    for(int i = 0; i < coordinates.count; i++)
    {
        [coordinates[i] getValue:&coords[i]];
    }
    self.polyline = [MAPolyline polylineWithCoordinates:coords count:coordinates.count];
    self.renderer = [[MAPolylineRenderer alloc] initWithPolyline:self.polyline];
    [self update];
}

- (void) update
{
    if (!_renderer) return;
    _renderer.fillColor = _fillColor;
    _renderer.strokeColor = _strokeColor;
    _renderer.lineWidth = _strokeWidth;
    _renderer.lineCapType = _lineCap;
    _renderer.lineJoinType = _lineJoin;
    _renderer.miterLimit = _miterLimit;
    _renderer.lineDash = _lineDash;
    
    if (_map == nil) return;
    [_map removeOverlay:self];
    [_map addOverlay:self];
}

#pragma mark MKOverlay implementation

- (CLLocationCoordinate2D) coordinate
{
    return self.polyline.coordinate;
}

-(MAMapRect)boundingMapRect{
    return self.polyline.boundingMapRect;
}

@end

//
//  AMapView.m
//  Awesome
//
//  Created by yunrui on 2017/3/13.
//  Copyright © 2017年 Facebook. All rights reserved.
//

#import "AMapView.h"
#import <React/UIView+React.h>
#import "AMapMarker.h"
#import "AMapPolyline.h"
#import "CoordinateQuadTree.h"
#import "Cluster.h"
#import "Convert2Json.h"
#import "AMapOverlay.h"

static NSInteger ZIndex(id obj)
{
    if ([obj conformsToProtocol:@protocol(AMapOverlay)]) {
        id<AMapOverlay> overlay = obj;
        return overlay.zIndex;
    }else{
        return 0;
    }
}

@interface MAMapView (UIGestureRecognizer)
// this tells the compiler that AMapView actually implements this method
- (BOOL)gestureRecognizer:(UIGestureRecognizer *)gestureRecognizer shouldReceiveTouch:(UITouch *)touch;
@end

@interface AMapView()<SMCalloutViewDelegate>
@property (nonatomic, strong) CoordinateQuadTree* coordinateQuadTree;
@property (nonatomic, assign) BOOL shouldCluster;
@end

@implementation AMapView
{
    NSMutableArray<UIView *> *_reactSubviews;
    BOOL _initialRegionSet;
}

-(instancetype)init
{
    if ((self = [super init])) {
        _reactSubviews = [NSMutableArray new];
        _callout = [SMCalloutView platformCalloutView];
        _callout.delegate = self;
    }
    return self;
}

- (void)dealloc
{
    [self.coordinateQuadTree clean];
}

-(CoordinateQuadTree*)coordinateQuadTree{
    if (!_coordinateQuadTree) {
        _coordinateQuadTree = [[CoordinateQuadTree alloc]init];
    }
    return _coordinateQuadTree;
}

#pragma mark RCTComponent

-(void)addOverlay:(id<MAOverlay>)overlay{
    if(![self.overlays count]){
        [super addOverlay:overlay];
    }
    NSInteger zIndex = ZIndex(overlay);
    __block NSUInteger index = self.overlays.count;
    [self.overlays enumerateObjectsUsingBlock:^(id  _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        NSInteger z = ZIndex(obj);
        if (z>zIndex) {
            index = idx;
            *stop = YES;
        }
    }];
    [self insertOverlay:overlay atIndex:index];
}

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wobjc-missing-super-calls"
- (void)insertReactSubview:(id<RCTComponent>)subview atIndex:(NSInteger)atIndex {
    if ([subview isKindOfClass:[AMapMarker class]]) {
        [self addAnnotation:(AMapMarker*)subview];
    } else if ([subview isKindOfClass:[AMapPolyline class]]){
        ((AMapPolyline*)subview).map = self;
        [self addOverlay:(AMapPolyline*)subview];
    }
    [_reactSubviews insertObject:(UIView *)subview atIndex:(NSUInteger) atIndex];
}

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wobjc-missing-super-calls"
- (void)removeReactSubview:(id<RCTComponent>)subview {
    if ([subview isKindOfClass:[AMapMarker class]]) {
        [self removeAnnotation:(AMapMarker*)subview];
    } else if ([subview isKindOfClass:[AMapPolyline class]]){
        [self removeOverlay:(AMapPolyline*)subview];
    }
    [_reactSubviews removeObject:(UIView *)subview];
}

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wobjc-missing-super-calls"
- (NSArray<id<RCTComponent>> *)reactSubviews {
    return _reactSubviews;
}

#pragma mark SMCalloutViewDelegate

- (NSTimeInterval)calloutView:(SMCalloutView *)calloutView delayForRepositionWithSize:(CGSize)offset {
    
    // When the callout is being asked to present in a way where it or its target will be partially offscreen, it asks us
    // if we'd like to reposition our surface first so the callout is completely visible. Here we scroll the map into view,
    // but it takes some math because we have to deal in lon/lat instead of the given offset in pixels.
    
    CLLocationCoordinate2D coordinate = self.region.center;
    
    // where's the center coordinate in terms of our view?
    CGPoint center = [self convertCoordinate:coordinate toPointToView:self];
    
    // move it by the requested offset
    center.x -= offset.width;
    center.y -= offset.height;
    
    // and translate it back into map coordinates
    coordinate = [self convertPoint:center toCoordinateFromView:self];
    
    // move the map!
    [self setCenterCoordinate:coordinate animated:YES];
    
    // tell the callout to wait for a while while we scroll (we assume the scroll delay for MKMapView matches UIScrollView)
    return kSMCalloutViewRepositionDelayForUIScrollView;
}

-(void)calloutViewClicked:(SMCalloutView *)calloutView{
    [self.selectedAnnotations enumerateObjectsUsingBlock:^(id  _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        if ([obj isKindOfClass:[AMapMarker class]]) {
            AMapMarker *marker = (AMapMarker*)obj;
            if (marker.onCalloutPress) {
                marker.onCalloutPress(@{});
            }
        }
    }];
}

#pragma mark Overrides for Callout behavior

// override UIGestureRecognizer's delegate method so we can prevent MKMapView's recognizer from firing
// when we interact with UIControl subclasses inside our callout view.


- (BOOL)gestureRecognizer:(UIGestureRecognizer *)gestureRecognizer shouldReceiveTouch:(UITouch *)touch {
    if ([touch.view isDescendantOfView:self.callout])
        return NO;
    else
        return [super gestureRecognizer:gestureRecognizer shouldReceiveTouch:touch];
}

// Allow touches to be sent to our calloutview.
// See this for some discussion of why we need to override this: https://github.com/nfarina/calloutview/pull/9
- (UIView *)hitTest:(CGPoint)point withEvent:(UIEvent *)event {
    
    UIView *calloutMaybe = [self.callout hitTest:[self.callout convertPoint:point fromView:self] withEvent:event];
    if (calloutMaybe) return calloutMaybe;
    
    return [super hitTest:point withEvent:event];
}

#pragma mark cluster

-(void)setClusterData:(NSArray<ClusterPoint *> *)clusterData{
    [clusterData enumerateObjectsUsingBlock:^(ClusterPoint * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        obj.idx = idx;
    }];
    _clusterData = clusterData;
    
    @synchronized(self)
    {
        self.shouldCluster = NO;
        __weak typeof(self) weakSelf = self;
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
            if (clusterData && clusterData.count) {
                /* 建立四叉树. */
                [weakSelf.coordinateQuadTree buildTreeWithPoints:clusterData];
            }else{
                [weakSelf.coordinateQuadTree clean];
            }
            
            weakSelf.shouldCluster = YES;
            [weakSelf calcuteCluster];
        });
    }
}

-(void)setClusterSize:(float)clusterSize{
    _clusterSize = clusterSize;
    [self calcuteCluster];
}

-(void)setOnCluster:(RCTDirectEventBlock)onCluster{
    _onCluster = onCluster;
    [self calcuteCluster];
}

-(void)calcuteCluster{
    @synchronized(self)
    {
        if (self.coordinateQuadTree.root == nil || !self.shouldCluster || self.clusterSize<=0 || !self.onCluster)
        {
            NSLog(@"cluster calcute not ready.");
            return;
        }
        
        /* 根据当前clusterSize和zoomScale 进行annotation聚合. */
        MAMapRect visibleRect = self.visibleMapRect;
        double zoomScale = self.bounds.size.width / visibleRect.size.width;
        
        __weak typeof(self) weakSelf = self;
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
            NSArray<Cluster *> *clustered = [weakSelf.coordinateQuadTree clusteredWithinMapRect:visibleRect zoomScale:zoomScale size:self.clusterSize];
            
            weakSelf.onCluster(@{@"clustered":[Convert2Json ClusterArray:clustered]});
        });
    }
}

- (void)setInitialRegion:(MACoordinateRegion)initialRegion {
    if (!_initialRegionSet) {
        _initialRegionSet = YES;
        [self setRegion:initialRegion animated:NO];
    }
}

- (void)setRegion:(MACoordinateRegion)region animated:(BOOL)animated{
    // If location is invalid, abort
    if (!CLLocationCoordinate2DIsValid(region.center)) {
        return;
    }
    
    // If new span values are nil, use old values instead
    if (!region.span.latitudeDelta) {
        region.span.latitudeDelta = self.region.span.latitudeDelta;
    }
    if (!region.span.longitudeDelta) {
        region.span.longitudeDelta = self.region.span.longitudeDelta;
    }
    
    // Animate/move to new position
    [super setRegion:region animated:animated];
}


@end

//
//  AMapMarker.m
//  Awesome
//
//  Created by yunrui on 2017/3/14.
//  Copyright © 2017年 Facebook. All rights reserved.
//

#import "AMapMarker.h"
#import <React/UIView+React.h>
#import <React/RCTImageLoader.h>
#import <React/RCTUtils.h>
#import "AMapCallout.h"

@interface AMapMarker()
@property(nonatomic,strong) AMapCallout *customCallout;
@end

@implementation AMapMarker{
  BOOL _hasSetCalloutOffset;
  RCTImageLoaderCancellationBlock _reloadImageCancellationBlock;
  MAPinAnnotationView *_pinView;
}

- (MAAnnotationView *)annotationView
{
  if ([self shouldUsePinView]) {
    if (!_pinView) {
      _pinView = [[MAPinAnnotationView alloc] initWithAnnotation:self reuseIdentifier:nil];
        [self addGestureRecognizerToView:_pinView];
      _pinView.annotation = self;
    }
    _pinView.draggable = self.draggable;
    _pinView.zIndex = self.zIndex;
      
    _pinView.centerOffset = self.centerOffset;
    _pinView.pinColor = self.pinColor;
    _pinView.canShowCallout = self.canShowCallout;
    return _pinView;
  } else {
    self.zIndex = self.zIndex;
    return self;
  }
}

- (BOOL)shouldUsePinView
{
  return self.reactSubviews.count == 0 && !self.imageSrc;
}

-(void)showCallout:(BOOL)animate{
    [self setSelected:YES animated:NO];
    MAAnnotationView *annotationView = self.annotationView;
    if (self.onSelect) self.onSelect(@{@"selected":@(YES)});
    if (!self.calloutEnabled || (!self.title && !self.subtitle && !self.customCallout)) {
        return;
    }
  [self fillCallout:self.mapView.callout];
    if ([self.mapView.callout.backgroundView isKindOfClass:[EmptyCalloutBackgroundView class]]) {
        [self.mapView.callout presentCalloutForceCenterFromRect:annotationView.bounds inView:annotationView constrainedToView:self.mapView animated:animate];
    }else{
        [self.mapView.callout presentCalloutFromRect:annotationView.bounds
                                              inView:annotationView
                                   constrainedToView:self.mapView
                                            animated:animate];
    }
}

-(void)dismissCallout:(BOOL)animate{
  [self.mapView.callout dismissCalloutAnimated:animate];
    [self setSelected:NO animated:NO];
    if (self.onSelect) self.onSelect(@{@"selected":@(NO)});
}

-(void)fillCallout:(SMCalloutView*)callout{
    if ([self shouldUsePinView] && !_hasSetCalloutOffset) {
        callout.calloutOffset = CGPointMake(-8,0);
    } else {
        callout.calloutOffset = self.calloutOffset;
    }
  if (self.customCallout) {
    callout.title = nil;
    callout.subtitle = nil;
      if (self.customCallout.tooltip) {
          // if tooltip is true, then the user wants their react view to be the "tooltip" as wwell, so we set
          // the background view to something empty/transparent
          callout.backgroundView = [EmptyCalloutBackgroundView new];
      } else {
          // the default tooltip look is wanted, and the user is just filling the content with their react subviews.
          // as a result, we use the default "masked" background view.
          callout.backgroundView = [SMCalloutMaskedBackgroundView new];
      }
    callout.contentView = self.customCallout;
  } else {
    callout.title = self.title;
    callout.subtitle = self.subtitle;
    callout.contentView = nil;
    callout.backgroundView = [SMCalloutMaskedBackgroundView new];
  }
}

//-(void)setSelected:(BOOL)selected animated:(BOOL)animated{
//  if (self.selected==selected) {
//    return;
//  }
//  [super setSelected:selected animated:animated];
//    if (self.onPress) {
//        self.onPress(nil);
//    }
//  if (self.onSelect) {
//    self.onSelect(@{@"selected":@(selected)});
//  }
//}
//
//-(void)setSelected:(BOOL)selected{
//  if (self.selected==selected) {
//    return;
//  }
//  [super setSelected:selected];
//  if (self.onSelect) {
//    self.onSelect(@{@"selected":@(selected)});
//  }
//}

#pragma mark - Tap Gesture & Events.

- (void)addTapGestureRecognizer {
    [self addGestureRecognizerToView:nil];
}

- (void)addGestureRecognizerToView:(UIView *)view {
    if (!view) {
        view = self;
    }
    UITapGestureRecognizer *tapGestureRecognizer = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(_handleTap:)];
    // setting this to NO allows the parent MapView to continue receiving marker selection events
    tapGestureRecognizer.cancelsTouchesInView = NO;
    [view addGestureRecognizer:tapGestureRecognizer];
}

- (void)_handleTap:(UITapGestureRecognizer *)recognizer {
    AMapMarker *marker = self;
    if (!marker) return;
    
    if (marker.selected) {
        CGPoint touchPoint = [recognizer locationInView:marker.mapView.callout];
        if ([marker.mapView.callout hitTest:touchPoint withEvent:nil]) {
            
            if (marker.onCalloutPress) marker.onCalloutPress(nil);
            return;
        }
    }
    
    if (marker.onPress) marker.onPress(nil);
    
    [marker.mapView selectAnnotation:marker animated:NO];
}

#pragma mark property setter

- (void)setCalloutOffset:(CGPoint)calloutOffset
{
    _hasSetCalloutOffset = YES;
    [super setCalloutOffset:calloutOffset];
}

- (void)setImageSrc:(NSString *)imageSrc
{
  _imageSrc = imageSrc;
  if (![imageSrc length]) {
    return;
  }
  if (_reloadImageCancellationBlock) {
    _reloadImageCancellationBlock();
    _reloadImageCancellationBlock = nil;
  }
  _reloadImageCancellationBlock = [self.bridge.imageLoader loadImageWithURLRequest:[RCTConvert NSURLRequest:_imageSrc]
                                                                              size:self.bounds.size
                                                                             scale:RCTScreenScale()
                                                                           clipped:YES
                                                                        resizeMode:RCTResizeModeCenter
                                                                     progressBlock:nil
                                                                  partialLoadBlock:nil
                                                                   completionBlock:^(NSError *error, UIImage *image) {
                                                                     NSLog(@"load image complete");
                                                                     if (error) {
                                                                       NSLog(@"%@", error);
                                                                     }
                                                                     dispatch_async(dispatch_get_main_queue(), ^{
                                                                       NSLog(@"image load success & set self.image %@",NSStringFromCGSize(image.size));
                                                                       self.image = image;
                                                                     });
                                                                   }];
}

-(void)setPinColor:(MAPinAnnotationColor)pinColor{
  _pinColor = pinColor;
  if (!_pinView) {
    _pinView.pinColor = pinColor;
  }
}

-(void)setCenterOffset:(CGPoint)centerOffset{
  [super setCenterOffset:centerOffset];
  if (!_pinView) {
    _pinView.centerOffset = centerOffset;
  }
}

-(void)setZIndex:(NSInteger)zIndex{
  [super setZIndex:zIndex];
  if (!_pinView) {
    _pinView.zIndex = zIndex;
  }
}

-(void)setCanShowCallout:(BOOL)canShowCallout{
  [super setCanShowCallout:canShowCallout];
  if (!_pinView) {
    _pinView.canShowCallout = canShowCallout;
  }
}

#pragma mark RCTComponent

- (void)reactSetFrame:(CGRect)frame
{
  // Make sure we use the image size when available
  CGSize size = self.image ? self.image.size : frame.size;
  CGRect bounds = {CGPointZero, size};
  
  // The MapView is basically in charge of figuring out the center position of the marker view. If the view changed in
  // height though, we need to compensate in such a way that the bottom of the marker stays at the same spot on the
  // map.
  CGFloat dy = (bounds.size.height - self.bounds.size.height) / 2;
  CGPoint center = (CGPoint){ self.center.x, self.center.y - dy };
  
  // Avoid crashes due to nan coords
  if (isnan(center.x) || isnan(center.y) ||
      isnan(bounds.origin.x) || isnan(bounds.origin.y) ||
      isnan(bounds.size.width) || isnan(bounds.size.height)) {
    RCTLogError(@"Invalid layout for (%@)%@. position: %@. bounds: %@",
                self.reactTag, self, NSStringFromCGPoint(center), NSStringFromCGRect(bounds));
    return;
  }
  
  self.center = center;
  self.bounds = bounds;
}

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wobjc-missing-super-calls"
- (void)insertReactSubview:(UIView<RCTComponent>*)subview atIndex:(NSInteger)atIndex {
  if ([subview isKindOfClass:[AMapCallout class]]) {
    self.customCallout = subview;
  }else{
    [super insertReactSubview:subview atIndex:atIndex];
  }
}

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wobjc-missing-super-calls"
- (void)removeReactSubview:(UIView<RCTComponent>*)subview {
  if ([subview isKindOfClass:[AMapCallout class]] && self.customCallout==subview) {
    self.customCallout = nil;
  }else{
    [super removeReactSubview:subview];
  }
}

@end

@implementation EmptyCalloutBackgroundView

@end

//
//  WalkNaviViewController.h
//  Pods
//
//  Created by yunrui on 2017/11/7.
//
//

#import <UIKit/UIKit.h>
#import <AMapNaviKit/AMapNaviKit.h>

@protocol WalkNaviViewControllerDelegate;

@interface WalkNaviViewController : UIViewController

@property(nonatomic,weak) id<WalkNaviViewControllerDelegate> delegate;

@property(nonatomic,strong)AMapNaviWalkView *walkView;

@end

@protocol WalkNaviViewControllerDelegate <NSObject>

-(void)walkNaviViewCloseButtonClicked;

@end

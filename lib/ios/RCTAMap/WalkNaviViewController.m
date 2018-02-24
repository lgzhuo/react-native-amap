//
//  WalkNaviViewController.m
//  Pods
//
//  Created by yunrui on 2017/11/7.
//
//

#import "WalkNaviViewController.h"

@interface WalkNaviViewController ()<AMapNaviWalkViewDelegate>

@end

@implementation WalkNaviViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    
    self.walkView.showMoreButton = NO;
    [self.walkView setFrame:self.view.bounds];
    [self.view addSubview:self.walkView];
}

- (void)viewWillAppear:(BOOL)animated
{
    [super viewWillAppear:animated];
    
    self.navigationController.navigationBarHidden = YES;
    self.navigationController.toolbarHidden = YES;
}

- (void)viewWillLayoutSubviews
{
    UIInterfaceOrientation interfaceOrientation = [[UIApplication sharedApplication] statusBarOrientation];
    if ([[[UIDevice currentDevice] systemVersion] floatValue] < 8.0)
    {
        interfaceOrientation = self.interfaceOrientation;
    }
    
    if (UIInterfaceOrientationIsPortrait(interfaceOrientation))
    {
        [self.walkView setIsLandscape:NO];
    }
    else if (UIInterfaceOrientationIsLandscape(interfaceOrientation))
    {
        [self.walkView setIsLandscape:YES];
    }
}

-(AMapNaviWalkView *)walkView{
    if (!_walkView) {
        _walkView = [[AMapNaviWalkView alloc]init];
        _walkView.autoresizingMask = UIViewAutoresizingFlexibleWidth|UIViewAutoresizingFlexibleHeight;
        _walkView.delegate = self;
    }
    return _walkView;
}


#pragma mark AMapNaviWalkViewDelegate

- (void)walkViewCloseButtonClicked:(AMapNaviWalkView *)walkView
{
    if (self.delegate && [self.delegate respondsToSelector:@selector(walkNaviViewCloseButtonClicked)])
    {
        [self.delegate walkNaviViewCloseButtonClicked];
    }
}

@end

#import "RNWebGLViewManager.h"
#import <React/RCTUIManager.h>
#import "RNWebGLView.h"
@implementation RNWebGLViewManager



RCT_EXPORT_VIEW_PROPERTY(onSurfaceCreate, RCTDirectEventBlock);
RCT_EXPORT_VIEW_PROPERTY(onDataReturned, RCTBubblingEventBlock)

RCT_EXPORT_VIEW_PROPERTY(msaaSamples, NSNumber);


RCT_EXPORT_METHOD(
                  capture:(nonnull NSNumber*) reactTag
                  width:(nonnull NSNumber*) width
                  height:(nonnull NSNumber*) height ) {
  [self.bridge.uiManager addUIBlock:^(RCTUIManager *uiManager, NSDictionary<NSNumber *,UIView *> *viewRegistry) {
    NSLog(@"config width");
    NSLog(@"%ld", (long)[width integerValue]);
    NSLog(@"config height");
    NSLog(@"%ld", (long)[height integerValue]);
     RNWebGLView *view = viewRegistry[reactTag];
    if (!view || ![view isKindOfClass:[RNWebGLView class]]) {
      RCTLogError(@"Cannot find NativeView with tag #%@", reactTag);
      return;
    }
    
    UIGraphicsBeginImageContextWithOptions(view.bounds.size, YES, 0);
    [view drawViewHierarchyInRect:view.bounds afterScreenUpdates:YES];
    UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    
    CGSize newSize = CGSizeMake([height doubleValue], [width doubleValue]);
    UIGraphicsBeginImageContext(newSize);
    [image drawInRect:CGRectMake(0,0,newSize.width,newSize.height)];
    UIImage* newImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    
    NSURL *tmpDirURL = [NSURL fileURLWithPath:NSTemporaryDirectory() isDirectory:YES];
    NSURL *fileURL = [[tmpDirURL URLByAppendingPathComponent:[[NSUUID UUID] UUIDString]] URLByAppendingPathExtension:@"png"];
    NSLog(@"fileURL: %@", [fileURL path]);
    NSData *imageData = UIImageJPEGRepresentation(newImage, 1);
    [imageData writeToFile:[fileURL path] atomically:YES];
    if (!view.onDataReturned) {
      return;
    }
        view.onDataReturned(@{

                                 @"url": [fileURL path]

                             });
  }];
}

//- (void)setOnDataReturned:(RCTDirectEventBlock)onDataReturned
//{
//  
//}




RCT_EXPORT_MODULE(RNWebGLViewManager);

- (UIView *)view
{
  RNWebGLView *view =  [[RNWebGLView alloc] initWithManager:self];
//  view.delegate = self;
  return view;
}


@end

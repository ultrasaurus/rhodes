//
//  PickImageDelegate.m
//  rhorunner
//
//  Created by Vlad on 2/12/09.
//  Copyright 2009 __MyCompanyName__. All rights reserved.
//

#import "PickImageDelegate.h"
#import "AppManager.h"

@implementation PickImageDelegate

@synthesize postUrl;


- (void)NotifyViewThreadRoutine:(id)object {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    
	// Get message body and its length
	NSData *postBody = (NSData*)object;	
	NSString *postLength = [NSString stringWithFormat:@"%d", [postBody length]];
	
	// Create post request
	NSMutableURLRequest *request = [[NSMutableURLRequest alloc] init];
	[request setURL:[NSURL URLWithString:postUrl]];
	[request setHTTPMethod:@"POST"];
	[request setValue:postLength forHTTPHeaderField:@"Content-Length"];
	[request setHTTPBody:postBody];
	
	// Send request
	[NSURLConnection sendSynchronousRequest:request returningResponse:nil error:nil];
	
    [pool release];
}

- (void)doCallback:(NSString*) message {
	// Create post body
	NSData* postBody = [message dataUsingEncoding:NSUTF8StringEncoding];
	// Start notification thread	
	[NSThread detachNewThreadSelector:@selector(NotifyViewThreadRoutine:)
							 toTarget:self withObject:postBody];		
}

- (void)useImage:(UIImage*)theImage { 
	NSString *folder = [[AppManager getApplicationsRootPath] stringByAppendingPathComponent:@"/public/db-files"];
	
	NSFileManager *fileManager = [NSFileManager defaultManager];
	if (![fileManager fileExistsAtPath:folder]) {
		[fileManager createDirectoryAtPath:folder attributes:nil];
	}
	
	NSString *now = [[[NSDate date] descriptionWithLocale:nil]
					 stringByReplacingOccurrencesOfString: @":" withString: @"."];
	now = [now stringByReplacingOccurrencesOfString: @" " withString: @"_"];
	now = [now stringByReplacingOccurrencesOfString: @"+" withString: @"_"];
	NSString *filename = [NSString stringWithFormat:@"Image_%@.png", now]; 	
	NSString *fullname = [folder stringByAppendingPathComponent:filename];
	NSData *pngImage = UIImagePNGRepresentation(theImage);
	 
	NSString* message;
	if([pngImage writeToFile:fullname atomically:YES]) {
		// Send new image uri to the view
		message = [@"status=ok&image_uri=%2Fpublic%2Fdb-files%2F" stringByAppendingString:filename];
	} else {
		// Notify view about error
		message = @"status=error&message=Can't write image to the storage.";
	}
	[self doCallback:message];
} 

- (void)imagePickerController:(UIImagePickerController *)picker 
		didFinishPickingImage:(UIImage *)image 
		editingInfo:(NSDictionary *)editingInfo 
{ 
	//If image editing is enabled and the user successfully picks an image, the image parameter of the 
	//imagePickerController:didFinishPicking Image:editingInfo:method contains the edited image. 
	//You should treat this image as the selected image, but if you want to store the original image, you can get 
	//it (along with the crop rectangle) from the dictionary in the editingInfo parameter. 
    [self useImage:image]; 
    // Remove the picker interface and release the picker object. 
    //[[picker parentViewController] dismissModalViewControllerAnimated:YES]; 
    [picker dismissModalViewControllerAnimated:YES];
	picker.view.hidden = YES;
    //[picker release]; 
} 

- (void)imagePickerControllerDidCancel:(UIImagePickerController *)picker 
{ 
	// Notify view about cancel
	NSString* message = @"status=cancel&message=User canceled operation.";
	[self doCallback:message];
	
    // Remove the picker interface and release the picker object. 
    [picker dismissModalViewControllerAnimated:YES]; 
	picker.view.hidden = YES;
    //[picker release]; 
} 

@end

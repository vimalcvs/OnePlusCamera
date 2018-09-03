#pragma version(1)
#pragma rs java_package_name(camera.vimal.vk)
#pragma rs_fp_relaxed

rs_allocation bitmap0;
rs_allocation bitmap1;
int step_size = 1;
int off_x = 0, off_y = 0;

int32_t *errors;

void init_errors() {
	for(int i=0;i<9;i++)
		errors[i] = 0;
}

void __attribute__((kernel)) align_mtb(uchar in, uint32_t x, uint32_t y) {

    x *= step_size;
    y *= step_size;
    if( x+off_x >= step_size && x+off_x < rsAllocationGetDimX(bitmap1)-step_size && y+off_y >= step_size && y+off_y < rsAllocationGetDimY(bitmap1)-step_size ) {
        uchar pixel0 = rsGetElementAt_uchar(bitmap0, x, y);
        int c=0;
        for(int dy=-1;dy<=1;dy++) {
            for(int dx=-1;dx<=1;dx++) {
            	uchar pixel1 = rsGetElementAt_uchar(bitmap1, x+off_x+dx*step_size, y+off_y+dy*step_size);
            	if( pixel0 != pixel1 ) {
            	    if( pixel0 != 127 && pixel1 != 127 )
                    	rsAtomicInc(&errors[c]);
            	}
                c++;
            }
        }
    }
}

void __attribute__((kernel)) align(uchar in, uint32_t x, uint32_t y) {

    x *= step_size;
    y *= step_size;
    if( x+off_x >= step_size && x+off_x < rsAllocationGetDimX(bitmap1)-step_size && y+off_y >= step_size && y+off_y < rsAllocationGetDimY(bitmap1)-step_size ) {
        float pixel0 = (float)rsGetElementAt_uchar(bitmap0, x, y);
        int c=0;
        for(int dy=-1;dy<=1;dy++) {
            for(int dx=-1;dx<=1;dx++) {
            	float pixel1 = (float)rsGetElementAt_uchar(bitmap1, x+off_x+dx*step_size, y+off_y+dy*step_size);
            	float diff = pixel1 - pixel0;
            	float diff2 = diff*diff;
            	if( errors[c] < 2000000000 ) {
                	rsAtomicAdd(&errors[c], diff2);
                }
                c++;
            }
        }
    }
}

#pragma version(1)
#pragma rs java_package_name(camera.vimal.vk)
#pragma rs_fp_relaxed

rs_allocation bitmap;


int32_t *sums;
int width;

void init_sums() {
	for(int i=0;i<width;i++)
		sums[i] = 0;
}

void __attribute__((kernel)) calculate_sharpness(uchar4 in, uint32_t x, uint32_t y) {
    int centre = in.g;
    int left = centre;
    int right = centre;
    int top = centre;
    int bottom = centre;

    if( x > 0 ) {
        left = rsGetElementAt_uchar4(bitmap, x-1, y).g;
    }
    if( x < width-1 ) {
        right = rsGetElementAt_uchar4(bitmap, x+1, y).g;
    }
    if( y > 0 ) {
        left = rsGetElementAt_uchar4(bitmap, x, y-1).g;
    }
    if( y < rsAllocationGetDimY(bitmap)-1 ) {
        right = rsGetElementAt_uchar4(bitmap, x, y+1).g;
    }

    int this_sum = abs((left + right + top + bottom - 4 * centre)/4);

	rsAtomicAdd(&sums[x], this_sum);
}

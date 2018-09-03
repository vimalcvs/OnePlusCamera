#pragma version(1)
#pragma rs java_package_name(camera.vimal.vk)
#pragma rs_fp_relaxed

rs_allocation bitmap;

static float black_level;
static float white_level;

void setBlackLevel(float value) {
    black_level = value;
    white_level = 255.0f / (255.0f - black_level);
}

float gain;
float gamma;


uchar4 __attribute__((kernel)) avg_brighten_gain(uchar4 in) {
    float3 value = gain*convert_float3(in.rgb);

	uchar4 out;
    out.rgb = convert_uchar3(clamp(value+0.5f, 0.f, 255.f));
    out.a = 255;
    return out;
}


uchar4 __attribute__((kernel)) avg_brighten_f(float3 rgb, uint32_t x, uint32_t y) {

    {

        float3 sum = 0.0;
         int radius = 1;
        int width = rsAllocationGetDimX(bitmap);
        int height = rsAllocationGetDimY(bitmap);
        int count = 0;

        for(int cy=y-radius;cy<=y+radius;cy++) {
            for(int cx=x-radius;cx<=x+radius;cx++) {
                if( cx >= 0 && cx < width && cy >= 0 && y < height ) {
                    float3 this_pixel = rsGetElementAt_float3(bitmap, cx, cy);
                    {
                                 const float C = 64.0f*64.0f/8.0f;

                        float3 diff = rgb - this_pixel;
                        float L = dot(diff, diff);

                        float weight = L/(L+C);

                        this_pixel = weight * rgb + (1.0-weight) * this_pixel;
                    }
                    sum += this_pixel;
                    count++;
                }
            }
        }

        rgb = sum / count;


    }

    rgb = rgb - black_level;
    rgb = rgb * white_level;
    rgb = clamp(rgb, 0.0f, 255.0f);



    rgb *= gain;
    float3 hdr = rgb;
    float value = fmax(hdr.r, hdr.g);
    value = fmax(value, hdr.b);
    if( value >= 0.5f ) {
        float new_value = powr(value/255.0f, gamma) * 255.0f;
        float gamma_scale = new_value / value;
        hdr *= gamma_scale;
    }
	uchar4 out;
    out.rgb = convert_uchar3(clamp(hdr+0.5f, 0.f, 255.f));
    out.a = 255;

    return out;
}

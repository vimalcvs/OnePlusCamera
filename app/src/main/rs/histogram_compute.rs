#pragma version(1)
#pragma rs java_package_name(camera.vimal.vk)
#pragma rs_fp_relaxed

int32_t *histogram;

void init_histogram() {
	for(int i=0;i<256;i++)
		histogram[i] = 0;
}

void __attribute__((kernel)) histogram_compute(uchar4 in, uint32_t x, uint32_t y) {
		uchar value = max(in.r, in.g);
	value = max(value, in.b);

	rsAtomicInc(&histogram[value]);
}

void __attribute__((kernel)) histogram_compute_f(float3 in_f, uint32_t x, uint32_t y) {
    uchar3 in;
    in.r = (uchar)clamp(in_f.r+0.5f, 0.0f, 255.0f);
    in.g = (uchar)clamp(in_f.g+0.5f, 0.0f, 255.0f);
    in.b = (uchar)clamp(in_f.b+0.5f, 0.0f, 255.0f);

	uchar value = max(in.r, in.g);
	value = max(value, in.b);

	rsAtomicInc(&histogram[value]);
}

void __attribute__((kernel)) histogram_compute_avg(uchar4 in, uint32_t x, uint32_t y) {
    float3 in_f = convert_float3(in.rgb);
    float avg = (in_f.r + in_f.g + in_f.b)/3.0;
    uchar value = (int)(avg+0.5);
	value = min(value, (uchar)255);

	rsAtomicInc(&histogram[value]);
}

void __attribute__((kernel)) histogram_compute_avg_f(float3 in_f, uint32_t x, uint32_t y) {
    float avg = (in_f.r + in_f.g + in_f.b)/3.0;
    uchar value = (int)(avg+0.5);
	value = min(value, (uchar)255);

	rsAtomicInc(&histogram[value]);
}

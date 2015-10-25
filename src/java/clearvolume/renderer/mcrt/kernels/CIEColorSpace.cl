// rgb2xyz and back
inline float3 XYZToRGB(const float3 xyz) {
    float3 rgb;
	rgb.x =  3.240479f*xyz.x - 1.537150f*xyz.y - 0.498535f*xyz.z;
	rgb.y = -0.969256f*xyz.x + 1.875991f*xyz.y + 0.041556f*xyz.z;
	rgb.z =  0.055648f*xyz.x - 0.204043f*xyz.y + 1.057311f*xyz.z;

	return rgb;
}

inline float3 RGBToXYZ(const float3 rgb) {
    float3 xyz;
	xyz.x = 0.412453f*rgb.x + 0.357580f*rgb.y + 0.180423f*rgb.z;
	xyz.y = 0.212671f*rgb.x + 0.715160f*rgb.y + 0.072169f*rgb.z;
	xyz.z = 0.019334f*rgb.x + 0.119193f*rgb.y + 0.950227f*rgb.z;

	return xyz;
}
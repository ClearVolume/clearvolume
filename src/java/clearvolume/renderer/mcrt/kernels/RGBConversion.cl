// convert float4 into uint:
inline
uint rgbaFloatToInt(float4 rgba)
{
    rgba = clamp(rgba,(float4)(0.f,0.f,0.f,0.f),(float4)(1.f,1.f,1.f,1.f));

    return ((uint)(rgba.w*255)<<24) | ((uint)(rgba.z*255)<<16) | ((uint)(rgba.y*255)<<8) | (uint)(rgba.x*255);
}

inline
int4 rgbaFloatToVInt(float4 rgba) {
    rgba = 255.0f*clamp(rgba,(float4)(0.f,0.f,0.f,0.f),(float4)(1.f,1.f,1.f,1.f));
    return convert_int4(rgba);
}

// convert float4 into uint and take the max with an existing RGBA value in uint form:
inline
uint rgbaFloatToIntAndMax(uint existing, float4 rgba)
{
    rgba = clamp(rgba,(float4)(0.f,0.f,0.f,0.f),(float4)(1.f,1.f,1.f,1.f));

    const uint nr = (uint)(rgba.x*255);
    const uint ng = (uint)(rgba.y*255);
    const uint nb = (uint)(rgba.z*255);
    const uint na = (uint)(rgba.w*255);

    const uint er = existing&0xFF;
    const uint eg = (existing>>8)&0xFF;
    const uint eb = (existing>>16)&0xFF;
    const uint ea = (existing>>24)&0xFF;

    const uint  r = max(nr,er);
    const uint  g = max(ng,eg);
    const uint  b = max(nb,eb);
    const uint  a = max(na,ea);

    return a<<24|b<<16|g<<8|r ;
}
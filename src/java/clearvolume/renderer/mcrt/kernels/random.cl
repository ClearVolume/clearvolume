// random number generator for dithering
inline
float random(uint x, uint y)
{
    uint a = 4421 +(1+x)*(1+y) +x +y;

    for(uint i=0; i < 10; i++)
    {
        a = ((uint)1664525 * a + (uint)1013904223) % (uint)79197919;
    }

    float rnd = (a*1.0f)/(79197919.f);

    return rnd-0.5f;
}

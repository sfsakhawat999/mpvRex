//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (Fujifilm F-Log2)

float fujifilm_flog2(float x) {
    const float a = 5.555556;
    const float b = 0.064829;
    const float c = 0.245281;
    const float d = 0.384316;
    const float e = 8.799461;
    const float f = 0.092864;
    const float cut1 = 0.000889;

    if (x < cut1) {
        return e * x + f;
    }
    return c * log(a * x + b) / log(10.0) + d;
}

vec3 fujifilm_flog2(vec3 color) {
    return vec3(
        fujifilm_flog2(color.r),
        fujifilm_flog2(color.g),
        fujifilm_flog2(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = fujifilm_flog2(color.rgb);

    return color;
}

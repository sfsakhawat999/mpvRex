//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (Fujifilm F-Log2, inverse)

float fujifilm_flog2_inv(float x) {
    const float a = 5.555556;
    const float b = 0.064829;
    const float c = 0.245281;
    const float d = 0.384316;
    const float e = 8.799461;
    const float f = 0.092864;
    const float cut2 = 0.100686685370811;

    if (x < cut2) {
        return (x - f) / e;
    }
    return pow(10.0, (x - d) / c) / a - b / a;
}

vec3 fujifilm_flog2_inv(vec3 color) {
    return vec3(
        fujifilm_flog2_inv(color.r),
        fujifilm_flog2_inv(color.g),
        fujifilm_flog2_inv(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = fujifilm_flog2_inv(color.rgb);

    return color;
}

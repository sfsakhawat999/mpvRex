//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (Fujifilm F-Log)

float fujifilm_flog(float x) {
    const float a = 0.555556;
    const float b = 0.009468;
    const float c = 0.344676;
    const float d = 0.790453;
    const float e = 8.735631;
    const float f = 0.092864;
    const float cut1 = 0.00089;

    if (x < cut1) {
        return e * x + f;
    }
    return c * log(a * x + b) / log(10.0) + d;
}

vec3 fujifilm_flog(vec3 color) {
    return vec3(
        fujifilm_flog(color.r),
        fujifilm_flog(color.g),
        fujifilm_flog(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = fujifilm_flog(color.rgb);

    return color;
}

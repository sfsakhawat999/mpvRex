//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (Fujifilm F-Log, inverse)

float fujifilm_flog_inv(float x) {
    const float a = 0.555556;
    const float b = 0.009468;
    const float c = 0.344676;
    const float d = 0.790453;
    const float e = 8.735631;
    const float f = 0.092864;
    const float cut2 = 0.1005377752;

    if (x < cut2) {
        return (x - f) / e;
    }
    return pow(10.0, (x - d) / c) / a - b / a;
}

vec3 fujifilm_flog_inv(vec3 color) {
    return vec3(
        fujifilm_flog_inv(color.r),
        fujifilm_flog_inv(color.g),
        fujifilm_flog_inv(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = fujifilm_flog_inv(color.rgb);

    return color;
}

//!HOOK OUTPUT
//!BIND HOOKED
//!DESC transfer function (Arri LogC3)

float arri_logc3(float x) {
    const float cut = 0.010591;
    const float a = 5.555556;
    const float b = 0.052272;
    const float c = 0.247190;
    const float d = 0.385537;
    const float e = 5.367655;
    const float f = 0.092809;

    if (x < cut) {
        return e * x + f;
    }
    return c * log(a * x + b) / log(10.0) + d;
}

vec3 arri_logc3(vec3 color) {
    return vec3(
        arri_logc3(color.r),
        arri_logc3(color.g),
        arri_logc3(color.b)
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = arri_logc3(color.rgb);

    return color;
}

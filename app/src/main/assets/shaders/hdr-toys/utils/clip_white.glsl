//!HOOK OUTPUT
//!BIND HOOKED
//!DESC clip code value (white)

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    color.rgb = min(color.rgb, 1.0);

    return color;
}

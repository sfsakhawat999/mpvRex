// Visualizes the out of gamut colors using false color.

//!HOOK OUTPUT
//!BIND HOOKED
//!DESC gamut mapping (false color)

float cbrt(float x) {
    return sign(x) * pow(abs(x), 1.0 / 3.0);
}

vec3 cbrt(vec3 color) {
    return vec3(
        cbrt(color.x),
        cbrt(color.y),
        cbrt(color.z)
    );
}

vec3 RGB_to_XYZ(vec3 RGB) {
    return RGB * mat3(
        0.6369580483012914, 0.14461690358620832,  0.1688809751641721,
        0.2627002120112671, 0.6779980715188708,   0.05930171646986196,
        0.000000000000000,  0.028072693049087428, 1.060985057710791
    );
}

vec3 XYZ_to_RGB(vec3 XYZ) {
    return XYZ * mat3(
         1.716651187971268,  -0.355670783776392, -0.253366281373660,
        -0.666684351832489,   1.616481236634939,  0.0157685458139111,
         0.017639857445311,  -0.042770613257809,  0.942103121235474
    );
}

vec3 XYZ_to_LMS(vec3 XYZ) {
    return XYZ * mat3(
        0.8190224379967030, 0.3619062600528904, -0.1288737815209879,
        0.0329836539323885, 0.9292868615863434,  0.0361446663506424,
        0.0481771893596242, 0.2642395317527308,  0.6335478284694309
    );
}

vec3 LMS_to_XYZ(vec3 LMS) {
    return LMS * mat3(
         1.2268798758459243, -0.5578149944602171,  0.2813910456659647,
        -0.0405757452148008,  1.1122868032803170, -0.0717110580655164,
        -0.0763729366746601, -0.4214933324022432,  1.5869240198367816
    );
}

vec3 LMS_to_Lab(vec3 LMS) {
    return LMS * mat3(
        0.2104542683093140,  0.7936177747023054, -0.0040720430116193,
        1.9779985324311684, -2.4285922420485799,  0.4505937096174110,
        0.0259040424655478,  0.7827717124575296, -0.8086757549230774
    );
}

vec3 Lab_to_LMS(vec3 Lab) {
    return Lab * mat3(
        1.0000000000000000,  0.3963377773761749,  0.2158037573099136,
        1.0000000000000000, -0.1055613458156586, -0.0638541728258133,
        1.0000000000000000, -0.0894841775298119, -1.2914855480194092
    );
}

vec3 RGB_to_Lab(vec3 color) {
    color = RGB_to_XYZ(color);
    color = XYZ_to_LMS(color);
    color = cbrt(color);
    color = LMS_to_Lab(color);
    return color;
}

vec3 Lab_to_RGB(vec3 color) {
    color = Lab_to_LMS(color);
    color = pow(color, vec3(3.0));
    color = LMS_to_XYZ(color);
    color = XYZ_to_RGB(color);
    return color;
}

vec3 BT2020_to_BT709(vec3 color) {
    return color * mat3(
         1.660491002108434,  -0.5876411387885491,  -0.072849863319885,
        -0.1245504745215906,  1.1328998971259596,  -0.0083494226043695,
        -0.0181507633549052, -0.1005788980080074,   1.118729661362913
    );
}

vec4 hook() {
    vec4 color = HOOKED_tex(HOOKED_pos);

    vec3 color_dst = BT2020_to_BT709(color.rgb);
    vec3 color_dst_cliped = clamp(color_dst, 0.0, 1.0);
    bool is_in_gamut = color_dst == color_dst_cliped;

    color.rgb = RGB_to_Lab(color.rgb);
    color.rgb = is_in_gamut ? vec3(color.x, vec2(0.0)) : vec3(0.5, color.yz);
    color.rgb = Lab_to_RGB(color.rgb);
    color.rgb = BT2020_to_BT709(color.rgb);

    return color;
}

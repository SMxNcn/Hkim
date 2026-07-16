#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec4 vertexColor;
in vec2 fragCoord;

out vec4 fragColor;

float roundedRectSDF(vec2 p, vec2 size, float r) {
    return length(max(abs(p) - size + r, 0.0)) - r;
}

void main() {
    vec4 rd = TextureMat[0];
    vec2 center = rd.xy + rd.zw * 0.5;
    vec2 halfSize = rd.zw * 0.5;
    vec2 p = fragCoord - center;

    float cornerRadius = TextureMat[1][0];
    float dist = roundedRectSDF(p, halfSize, cornerRadius);
    float outerAlpha = 1.0 - smoothstep(-0.25, 0.25, dist);

    vec4 fillColor = vertexColor;
    float borderWidth = TextureMat[1][1];

    if (borderWidth > 0.0) {
        float innerR = max(cornerRadius - borderWidth + 0.35, 0.0);
        vec2 innerHalf = max(halfSize - borderWidth, vec2(0.0));
        float innerDist = roundedRectSDF(p, innerHalf, innerR);
        float innerAlpha = 1.0 - smoothstep(-0.25, 0.25, innerDist);
        float borderMask = outerAlpha - innerAlpha;

        vec4 borderColor = vec4(TextureMat[1][2], TextureMat[1][3], TextureMat[2][0], TextureMat[2][1]);

        float fillA = fillColor.a * innerAlpha;
        vec3 fillPm = fillColor.rgb * fillA;

        float borderA = borderColor.a * borderMask;
        vec3 borderPm = borderColor.rgb * borderA;

        float outA = borderA + fillA * (1.0 - borderA);
        vec3 outPm = borderPm + fillPm * (1.0 - borderA);

        vec3 outRgb = (outA > 0.001) ? outPm / outA : vec3(0.0);

        if (outA < 0.01) discard;
        fragColor = vec4(outRgb, outA);
    } else {
        if (outerAlpha < 0.01) discard;
        fragColor = vec4(fillColor.rgb, fillColor.a * outerAlpha);
    }
}

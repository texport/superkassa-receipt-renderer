// swift-tools-version:5.5
import PackageDescription

let package = Package(
    name: "SuperkassaReceiptRenderer",
    platforms: [
        .iOS(.v15)
    ],
    products: [
        .library(
            name: "SuperkassaReceiptRenderer",
            targets: ["SuperkassaReceiptRenderer"]
        ),
    ],
    dependencies: [],
    targets: [
        .binaryTarget(
            name: "SuperkassaReceiptRenderer",
            url: "https://github.com/texport/superkassa-receipt-renderer/releases/download/v1.0.7/SuperkassaReceiptRenderer.xcframework.zip",
            checksum: "8ef2d87b8a4aad1a2a7c234b5dbe27a122def1db53862a45f351f471c19b1357"
        )
    ]
)

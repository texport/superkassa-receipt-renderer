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
            url: "https://github.com/texport/superkassa-receipt-renderer/releases/download/v1.0.4/SuperkassaReceiptRenderer.xcframework.zip",
            checksum: "758a05cb022d802617f595d688a5cb8067aa3e12760920d91055ee78a809bd50"
        )
    ]
)

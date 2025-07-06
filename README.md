# ContentProvider File Sharing Demo

This project demonstrates how to use an Android `ContentProvider` to facilitate file sharing between two separate applications within an AOSP environment.

## Project Goal

The primary goal of this project is to provide a clear, runnable example of inter-app file sharing using `ContentProvider` in Android. It showcases the necessary components and configurations for a `ContentProvider` to expose files, and for client applications to access and manipulate those files securely.

## Components

This project consists of three main applications:

1.  **`SharedStorageProvider` (ContentProvider)**:
    -   This application acts as the central file provider. It exposes a `ContentProvider` that allows other applications to access files stored within its designated shared directory.
    -   It handles file creation, reading, writing, and deletion requests from client applications, ensuring proper permissions and security.

2.  **`ClientAppA` (Client Application)**:
    -   This is a client application that interacts with the `SharedStorageProvider`.
    -   It demonstrates how to use `ContentResolver` to perform file operations (create, write, read, delete, list) on files managed by the `SharedStorageProvider`.

3.  **`ClientAppB` (Client Application)**:
    -   Similar to `ClientAppA`, this is another client application that interacts with the `SharedStorageProvider`.
    -   It serves to demonstrate that multiple independent applications can securely share and access the same files through the `ContentProvider`.

## How ContentProvider Enables File Sharing

Android's `ContentProvider` offers a structured and secure way for applications to share data, including files, with other applications. In this demo, `SharedStorageProvider` acts as the data source, and `ClientAppA` and `ClientAppB` are the data consumers.

Here's a breakdown of the mechanism:

1.  **Content URI (Uniform Resource Identifier)**:
    -   The `SharedStorageProvider` defines a unique Content URI (e.g., `content://com.example.sharedstorageprovider.fileprovider/files/`) that represents the data it can share. Client applications use this URI to identify the specific data they want to access.

2.  **`ContentResolver` (Client-Side Access)**:
    -   Client applications (`ClientAppA` and `ClientAppB`) do not directly interact with the `ContentProvider`. Instead, they use a `ContentResolver` object. The `ContentResolver` acts as an intermediary, sending requests (e.g., `openFile`, `query`, `insert`, `update`, `delete`) to the `ContentProvider` based on the provided Content URI.

3.  **`ContentProvider` (Server-Side Logic)**:
    -   When the `ContentResolver` sends a request, the Android system routes it to the appropriate `ContentProvider` (in this case, `SharedStorageProvider`) based on the URI's authority.
    -   The `SharedStorageProvider` then implements methods like `openFile`, `query`, `insert`, `update`, and `delete` to handle these requests. For file sharing, the `openFile` method is crucial. It returns a `ParcelFileDescriptor`, which is a file descriptor that can be passed across processes.

4.  **Secure File Descriptors (`ParcelFileDescriptor`)**:
    -   Instead of sharing direct file paths (which would be insecure and prone to permission issues), `ContentProvider` shares `ParcelFileDescriptor` objects. This allows the client application to access the underlying file stream without knowing its actual path on the provider's filesystem. The Android system manages the security and permissions of this shared descriptor.

5.  **Permissions Enforcement**:
    -   The `SharedStorageProvider` declares specific permissions in its `AndroidManifest.xml` (e.g., `android:readPermission`, `android:writePermission`). Client applications must request these permissions to access the provider's data. This ensures that only authorized applications can interact with the shared files.

6.  **Inter-Process Communication (IPC)**:
    -   The entire interaction between the `ContentResolver` in the client app and the `ContentProvider` in the provider app happens via Binder IPC (Inter-Process Communication), a secure and efficient mechanism provided by the Android system.

In summary, `ContentProvider` abstracts away the complexities of inter-process communication and file system access, providing a secure, standardized interface for applications to share files. The `SharedStorageProvider` manages the files, and `ClientAppA` and `ClientAppB` access them through the `ContentResolver` and the `ContentProvider`'s defined URIs and permissions.

This project was created with the assistance of gemini-cli.

## License

This project is open-source and available under the Apache 2.0 License. See the `LICENSE` file for more details.
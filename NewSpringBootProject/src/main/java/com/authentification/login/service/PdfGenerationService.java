package com.authentification.login.service;

import com.authentification.login.entitiy.Abonnement;
import com.authentification.login.entitiy.User;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

@Service
public class PdfGenerationService {

    private byte[] downloadUserPhoto(String photoUrl) throws Exception {
        URL url = new URL(photoUrl);
        try (InputStream inputStream = url.openStream()) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            // Vérifier si les données téléchargées représentent une image valide
            Image.getInstance(outputStream.toByteArray()); // Cela lèvera une exception si les données ne représentent pas une image valide
            return outputStream.toByteArray();
        }
    }

    public byte[] generateUserCardPdf(User user, Abonnement abonnement) throws Exception {
        // Télécharger la photo de l'utilisateur à partir de l'URL
        String photoUrl = "https://cdn-icons-png.flaticon.com/512/3177/3177440.png";
        byte[] userPhoto = downloadUserPhoto(photoUrl);

        // Créer un nouveau document PDF
        Document document = new Document();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, outputStream);

        // Ouvrir le document pour écrire
        document.open();

        // Ajouter le contenu au document
        Paragraph title = new Paragraph("User Card", FontFactory.getFont(FontFactory.HELVETICA, 18, Font.BOLD));
        title.setAlignment(Element.ALIGN_CENTER); // Aligner le titre au centre
        document.add(title);

        // Ajouter un espacement après le titre
        document.add(new Paragraph("\n"));
        if (userPhoto != null && userPhoto.length > 0) {
            Image image = Image.getInstance(userPhoto);
            image.setAlignment(Element.ALIGN_RIGHT); // Aligner l'image au centre
            image.scaleToFit(100, 100); // Redimensionner l'image si nécessaire
            document.add(image);
        }

        // Ajouter les informations de l'utilisateur au document
        Paragraph userInfo = new Paragraph();
        userInfo.add(new Chunk("Nom : ", FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD)));
        userInfo.add(user.getLastname() + "\n");
        userInfo.add(new Chunk("Prénom : ", FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD)));
        userInfo.add(user.getFirstname() + "\n");
        userInfo.add(new Chunk("E-mail : ", FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD)));
        userInfo.add(user.getEmail() + "\n");
        userInfo.add(new Chunk("Nom d'utilisateur : ", FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD)));
        userInfo.add(user.getUsername() + "\n");
        userInfo.add(new Chunk("Date d'inscription : ", FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD)));
        userInfo.add(abonnement.getDate_inscription() + "\n");
        userInfo.add(new Chunk("Date d'expiration : ", FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD)));
        userInfo.add(abonnement.getDate_expiration()+ "\n");

        document.add(userInfo);

        // Ajouter un espacement après les informations de l'utilisateur
        document.add(new Paragraph("\n"));

        // Ajouter la photo de l'utilisateur au document


        // Fermer le document
        document.close();

        return outputStream.toByteArray();
    }
}

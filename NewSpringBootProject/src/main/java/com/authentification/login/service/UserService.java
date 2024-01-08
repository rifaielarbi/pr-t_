package com.authentification.login.service;

import com.authentification.login.DTO.MailStructure;
import com.authentification.login.Exeption.Exeptions;
import com.authentification.login.JWT.JWTUtil;
import com.authentification.login.JWT.JWTfilter;
import com.authentification.login.JWT.UtilisateurDetails;
import com.authentification.login.entitiy.*;
import com.authentification.login.repository.*;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class UserService implements UserServiceImp {
    @Autowired
    private UserRepo userrepo;

    @Autowired
    private LecteurRepo lecteurRepo;

    @Autowired
    private EmployeRepo employeRepo;

    @Autowired
    private AbonnementRepo abonnementRepo;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UtilisateurDetails utilisateurDetails;

    @Autowired
    JWTUtil jwtUtil;

    @Autowired
    JWTfilter jwTfilter;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private MailService mailService;

    @Autowired
    private PdfGenerationService pdfGenerationService;

    @Autowired
    private PosteRepo posteRepo;


    @Override
    public ResponseEntity<String> enregistrer(Map<String, String> RequestMap) {
        log.info("dans enregistrement{}", RequestMap);
        try {
            if (validationdeenregistrement(RequestMap)) {
                String email = RequestMap.get("email");
                User existingUser = userrepo.findByEmailId(email);
                if (existingUser == null) {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setFirstname(RequestMap.get("firstname"));
                    newUser.setLastname(RequestMap.get("lastname"));
                    newUser.setPassword(passwordEncoder.encode(RequestMap.get("password")));
                    newUser.setStatus("false");
                    newUser.setRole("user");
                    newUser.setUsername(RequestMap.get("username"));
                    userrepo.save(newUser);

                    Lecteur lecteur = new Lecteur();
                    lecteur.setUser(newUser);
                    lecteurRepo.save(lecteur);

                    Abonnement abonnement =  new Abonnement();
                    abonnement.setSolde(Long.valueOf(RequestMap.get("solde")));
                    // Création d'un objet LocalDate pour la date actuelle
                    LocalDate dateInscription = LocalDate.now();

                    // Ajout de 30 jours à la date actuelle pour obtenir la date d'expiration
                    LocalDate dateExpiration = dateInscription.plusMonths(1);

                    // Conversion des dates en chaînes de caractères au format "année-mois-jour"
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    String dateInscriptionStr = dateInscription.format(formatter);
                    String dateExpirationStr = dateExpiration.format(formatter);

                   // Mise à jour des dates d'inscription et d'expiration de l'abonnement en utilisant des dates Java classiques
                    abonnement.setDate_inscription(java.sql.Date.valueOf(dateInscriptionStr));
                    abonnement.setDate_expiration(java.sql.Date.valueOf(dateExpirationStr));
                    abonnement.setLecteur(lecteur);
                    abonnementRepo.save(abonnement);

                    byte[] pdfBytes = pdfGenerationService.generateUserCardPdf(newUser,abonnement);
                    sendUserPdfByEmail(email, pdfBytes);

                    return Exeptions.getResposeEntity("Le registrement est réussi. Le PDF a été envoyé par e-mail.", HttpStatus.OK);
                } else {
                    return Exeptions.getResposeEntity("Email déjà existant.", HttpStatus.BAD_REQUEST);
                }
            } else {
                return Exeptions.getResposeEntity("Données invalides.", HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Exeptions.getResposeEntity("Une erreur s'est produite lors de l'enregistrement.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void sendUserPdfByEmail(String userEmail, byte[] pdfBytes) {
        try {
            mailService.sendPdfMail(userEmail, pdfBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public ResponseEntity<String> enregistrerEmploye(Map<String, String> RequestMap) {

        log.info("dans enregistrement{}", RequestMap); //il va affiche les donnees dans le journal sous forme des donnes entree
        try {
//                si les donnees sont entree dans RequestMap ou non
            if (validationdeenregistrement(RequestMap) && jwTfilter.isAdmin()) {
                User user = userrepo.findByEmailId(RequestMap.get("email"));
                // si les donnees n'ont pas dans la base de donnees
                if (Objects.isNull(user)) {
//                     ajouter les donnees
                    User userV = new User();
                    userV.setEmail(getUserRequest(RequestMap).getEmail());
                    userV.setFirstname(getUserRequest(RequestMap).getFirstname());
                    userV.setLastname(getUserRequest(RequestMap).getLastname());
                    userV.setPassword(passwordEncoder.encode(getUserRequest(RequestMap).getPassword()));
                    userV.setStatus("true");
                    userV.setRole("employe");
                    userV.setUsername(getUserRequest(RequestMap).getUsername());
                    userrepo.save(userV);

                    Employe employe = new Employe();
                    employe.setUser(userV);
                    employeRepo.save(employe);


                    return Exeptions.getResposeEntity("le registrement est reussit",HttpStatus.OK);

                } else {
                    return Exeptions.getResposeEntity("Email deja existe",HttpStatus.BAD_REQUEST);
                }

            } else {
                return Exeptions.getResposeEntity("donnee invalidee",HttpStatus.BAD_REQUEST);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return Exeptions.getResposeEntity("les donnees sont echouees",HttpStatus.INTERNAL_SERVER_ERROR);
    }


    public boolean validationdeenregistrement(Map<String, String> RequestMap) {
        if (RequestMap.containsKey("firstname") && RequestMap.containsKey("lastname") && RequestMap.containsKey("username") && RequestMap.containsKey("email")
                && RequestMap.containsKey("password")  ) {
            return true;
        } else
            return false;
    }

    public User getUserRequest(Map<String, String> RequestMap) {
        User user = new User();
        user.setFirstname(RequestMap.get("firstname"));
        user.setLastname(RequestMap.get("lastname"));
        user.setEmail(RequestMap.get("email"));
        user.setPassword(RequestMap.get("password"));
        user.setUsername(RequestMap.get("username")); // Assurez-vous que "username" est présent dans RequestMap
        user.setStatus("false");
        user.setRole("user");
        return user;
    }

    @Override
    public ResponseEntity<String> login(Map<String, String> RequestMap) {
        log.info("Inside login{}",RequestMap);
        try{
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(RequestMap.get("email"), RequestMap.get("password"))
            );

            if (auth.isAuthenticated()) {
                if (utilisateurDetails.getUserEntity().getStatus().equalsIgnoreCase("true")) {
                    // Vérifier si le mot de passe correspond en utilisant matches
                    if (passwordEncoder.matches(RequestMap.get("password"), utilisateurDetails.getUserEntity().getPassword())) {
                        return new ResponseEntity<>("{\"token\":\"" + jwtUtil.generateToken(utilisateurDetails.getUserEntity().getEmail(), utilisateurDetails.getUserEntity().getRole()) + "\", \"role\":\"" + utilisateurDetails.getUserEntity().getRole() + "\", \"username\":\"" + utilisateurDetails.getUserEntity().getUsername() +  "\", \"email\":\"" + utilisateurDetails.getUserEntity().getEmail() +"\"}", HttpStatus.OK);
                     //   return new ResponseEntity<>("{\"token\":\"" + jwtUtil.generateToken(utilisateurDetails.getUserEntity().getEmail(), utilisateurDetails.getUserEntity().getRole()) + "\", \"role\":\"" + utilisateurDetails.getUserEntity().getRole()  + "\"}", HttpStatus.OK);

                    }
                } else {
                    return new ResponseEntity<String>("{\"message\":\"" + "Attendez l'approbation de l'administrateur" + "\"}", HttpStatus.BAD_REQUEST);
                }
            }
        }catch (Exception e){
            log.error("{}",e);
        }
        return new ResponseEntity<String>("{\"message\":\""+"Authentification echouee"+"\"}",HttpStatus.BAD_REQUEST );
    }

    @Override
    public ResponseEntity<List<User>> getAllUser() {
        try {
          if(jwTfilter.isAdmin()){
             return new ResponseEntity<>(userrepo.getAllUser(),HttpStatus.OK);
          }else {
              return new ResponseEntity<>(new ArrayList<>(),HttpStatus.UNAUTHORIZED);
          }
        }catch (Exception e){
            e.printStackTrace();
        }
        return new ResponseEntity<>(new ArrayList<>(),HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<List<User>> getdemandes() {
        try {
            if(jwTfilter.isAdmin()){
                return new ResponseEntity<>(userrepo.getDemandes(),HttpStatus.OK);
            }else {
                return new ResponseEntity<>(new ArrayList<>(),HttpStatus.UNAUTHORIZED);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return new ResponseEntity<>(new ArrayList<>(),HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    @Transactional
    public ResponseEntity<String> update(Map<String, String> requestMap) {
        try {
            if (jwTfilter.isAdmin()) {
                Long userId = Long.parseLong(requestMap.get("id"));
                Optional<User> optionalUser = userrepo.findById(userId);
                if (optionalUser.isPresent()) {
                    userrepo.updateStatus("true", userId);
                    return Exeptions.getResposeEntity("Le statut a été modifié avec succès", HttpStatus.OK);
                } else {
                    return Exeptions.getResposeEntity("L'identifiant de l'utilisateur n'existe pas", HttpStatus.NOT_FOUND);
                }
            } else {
                return Exeptions.getResposeEntity("Les données ne sont pas autorisées", HttpStatus.UNAUTHORIZED);
            }
        } catch (NumberFormatException e) {
            return Exeptions.getResposeEntity("L'identifiant de l'utilisateur n'est pas valide", HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            e.printStackTrace();
            return Exeptions.getResposeEntity("Une erreur interne s'est produite", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Override
    public ResponseEntity<String> changePasswd(Map<String, String> RequestMap) {
        log.info("Inside change passwd{}", RequestMap);
        try {
            if (jwTfilter.isAdmin() || jwTfilter.isUser() || jwTfilter.isEmploye()) {
                Optional<User> optional = Optional.ofNullable(userrepo.findByEmailId(jwTfilter.getMailbyToken()));
                if (optional.isPresent()) {
                    User user = optional.get();
                    // Vérifiez si l'ancien mot de passe correspond
                    if (passwordEncoder.matches(RequestMap.get("old_password"), user.getPassword())) {
                        String newPassword = RequestMap.get("new_password");
                        String confirmNewPassword = RequestMap.get("confirm_password");
                        // Vérifiez si le nouveau mot de passe correspond à la confirmation du mot de passe
                        if (newPassword.equals(confirmNewPassword)) {
                            // Mettez à jour le mot de passe avec le nouveau mot de passe encodé
                            user.setPassword(passwordEncoder.encode(newPassword));
                            userrepo.save(user);
                            return Exeptions.getResposeEntity("Le mot de passe a été modifié avec succès", HttpStatus.OK);
                        } else {
                            return Exeptions.getResposeEntity("Le nouveau mot de passe ne correspond pas à la confirmation du mot de passe", HttpStatus.BAD_REQUEST);
                        }
                    } else {
                        return Exeptions.getResposeEntity("L'ancien mot de passe est incorrect", HttpStatus.BAD_REQUEST);
                    }
                } else {
                    return Exeptions.getResposeEntity("L'utilisateur n'existe pas", HttpStatus.NOT_FOUND);
                }
            } else {
                return Exeptions.getResposeEntity("Accès non autorisé", HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Exeptions.getResposeEntity("Une erreur s'est produite lors de la modification du mot de passe", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



    @Override
    public ResponseEntity<String> motDePaseeOublie(Map<String, String> requestMap) {
        log.info("Inside mot de passe oublie{}", requestMap);
        try {
            String email = requestMap.get("email");
            Optional<User> optionalUser = Optional.ofNullable(userrepo.findByEmailId(email));
            if (optionalUser.isPresent()) {
                User user = optionalUser.get();
                String resetCode = generateResetCode();
                user.setPassword(passwordEncoder.encode(resetCode));
                userrepo.save(user);

                String mailSubject = "Réinitialisation de votre mot de passe";
                String mailMessage = buildResetMailContent(resetCode);
                MailStructure mailStructure = new MailStructure(mailSubject, mailMessage);
                mailService.sendMail(email, mailStructure);

                return Exeptions.getResposeEntity("Un e-mail de réinitialisation a été envoyé à votre adresse e-mail enregistrée.", HttpStatus.OK);
            } else {
                return Exeptions.getResposeEntity("Aucun utilisateur n'est enregistré avec cette adresse e-mail.", HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Exeptions.getResposeEntity("Une erreur s'est produite lors de la demande de réinitialisation de mot de passe.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // Méthode pour générer un code de réinitialisation de mot de passe aléatoire
// Méthode pour générer un code de réinitialisation de mot de passe aléatoire
    private String generateResetCode() {
        // Générer un code aléatoire de réinitialisation (par exemple, une chaîne aléatoire)
        // Vous pouvez utiliser une logique de génération de code aléatoire personnalisée ici
        // Exemple simple :
        int length = 8;
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder resetCode = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            resetCode.append(characters.charAt(random.nextInt(characters.length())));
        }
        return resetCode.toString();
    }

    // Méthode pour construire le contenu de l'e-mail de réinitialisation de mot de passe
    private String buildResetMailContent(String resetCode) {
        // Construire le contenu de l'e-mail avec le code de réinitialisation
        String mailContent = "Bonjour,\n\n";
        mailContent += "Vous avez demandé à réinitialiser votre mot de passe. Utilisez le code suivant pour réinitialiser votre mot de passe :\n\n";
        mailContent += "Code de réinitialisation : " + resetCode + "\n\n";
        mailContent += "Si vous n'avez pas demandé de réinitialisation de mot de passe, vous pouvez ignorer cet e-mail.\n\n";
        mailContent += "Cordialement,\nVotre équipe de support";
        return mailContent;
    }


    @PostConstruct
    public void initialiseApp() {

            User user =new User();
            user.setEmail("arabi@gmail.com");
            user.setFirstname("arabi");
            user.setUsername("arabi rifay");
            user.setLastname("rifay");
            user.setRole("admin");
            user.setStatus("true");
            user.setPassword(new BCryptPasswordEncoder().encode("12345678"));
            userrepo.save(user);


        User user2 =new User();
        user2.setEmail("yahia@gmail.com");
        user2.setFirstname("yahia");
        user2.setUsername("yahia karoum");
        user2.setLastname("karoum");
        user2.setRole("employe");
        user2.setStatus("true");
        user2.setPassword(new BCryptPasswordEncoder().encode("12345678"));
        userrepo.save(user2);


        User user3 =new User();
        user3.setEmail("amine@gmail.com");
        user3.setFirstname("amine");
        user3.setUsername("amine benncar");
        user3.setLastname("benncar");
        user3.setRole("user");
        user3.setStatus("true");
        user3.setPassword(new BCryptPasswordEncoder().encode("12345678"));
        userrepo.save(user3);

        Lecteur lecteur = new Lecteur();
        lecteur.setUser(user3);
        lecteurRepo.save(lecteur);

        Abonnement abonnement =  new Abonnement();
        abonnement.setSolde(1000L);
        LocalDate dateInscription = LocalDate.now();
        LocalDate dateExpiration = dateInscription.plusMonths(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String dateInscriptionStr = dateInscription.format(formatter);
        String dateExpirationStr = dateExpiration.format(formatter);
        abonnement.setDate_inscription(java.sql.Date.valueOf(dateInscriptionStr));
        abonnement.setDate_expiration(java.sql.Date.valueOf(dateExpirationStr));
        abonnement.setLecteur(lecteur);
        abonnementRepo.save(abonnement);



        for (int i = 1; i <= 12; i++) {
            Poste poste = new Poste();
            poste.setNumero(i);
            poste.setEtat(true);

            posteRepo.save(poste);
        }

        }

    }

